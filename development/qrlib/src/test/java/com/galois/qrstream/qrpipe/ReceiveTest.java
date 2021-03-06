/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.qrpipe;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.galois.qrstream.image.BitmapImage;
import com.galois.qrstream.image.YuvImage;
import com.google.common.base.Charsets;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;

public class ReceiveTest {

  /**
   * Simple IProgress implementation that does nothing with it's arguments.
   */
  private static final IProgress NULL_MONITOR = new IProgress() {
    @Override
    public void changeState(DecodeState state) {
    }
    @Override
    public void drawFinderPoints(float[] pts){
    }};

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  /**
   * Limited test to see if the YUV generator works.
   * @throws ReceiveException
   */
  @Test
  public void testYUVGeneration() throws ReceiveException {
    String filename   = "fooScreenshot_withReservedBits.png";

    // decode a QR code to get an oracle:
    Result result = decodeAndCheckValidQR(filename);

    // Create a yuvData array & YuvImage based on the same image:
    BufferedImage image = getImageResourceAndCheckNotNull(filename);
    byte[] yuvData = YuvUtilities.toYUV(image);
    YuvImage yuvImage = new YuvImage(yuvData, image.getWidth(), image.getHeight());

    // Use receive to decode this qr code:
    Receive receive = new Receive(image.getHeight(), image.getWidth(), NULL_MONITOR);

    byte[] actual = receive.decodeQRCodes(new FrameProvider(yuvImage));
    byte[] expected = PartialMessage.createFromResult(result, Integer.MAX_VALUE).getPayload();

    assertArrayEquals("Yuv data generated different results", expected, actual);
  }

  /**
   * Check that Recieve.decodeQRCodes(...) can throw an exception
   * if no more data arrives.  This test fails if no exception is thrown within
   * 400ms.
   *
   * @throws ReceiveException
   */
  @Test(timeout=400, expected=ReceiveException.class)
  public void testDecodeQRThrowsOnEmptyQueue() throws ReceiveException {

    Receive receiver = new Receive(640, 480, NULL_MONITOR);

    byte[] message = receiver.decodeQRCodes(FrameProvider.INVALID_COLLECTION);
    System.out.println("length: message=" + message.length);
  }

  /**
   * Check that single QR code that we expect to have sequence
   * information inserted into its payload really does have it.
   */
  @Test
  public void testDecodeQrHasSequenceInfo() {
    // Decode qr code received from transmitter as screenshot
    String containsChunkInfo   = "fooScreenshot_withReservedBits.png";
    String containsNoChunkInfo = "fooScreenshot_noReservedBits.png";

    // Expect decoded image to have my chunk info prepended to payload
    Result resultWithChunk = decodeAndCheckValidQR(containsChunkInfo);

    // This image does not have chunk info in it
    Result resultNoChunk = decodeAndCheckValidQR(containsNoChunkInfo);

    // The encoded QR codes should have different byte data
    assertFalse("Decoded image missing chunkInfo",
        Arrays.equals(resultNoChunk.getRawBytes(), resultWithChunk.getRawBytes()));
  }

  /**
   * Try decoding standard QR code without any of the
   * sequence information inserted into payload.
   */
  @Test
  public void testDecodeQrWithNoSequenceInfo() {
    // Decode qr code received from transmitter as screenshot
    String filename = "fooScreenshot_noReservedBits.png";
    String expectedText   = "foo";

    BufferedImage b = getImageResourceAndCheckNotNull(filename);
    LuminanceSource lumSrc = new BufferedImageLuminanceSource(b);

    assertNotNull("Unable to convert BufferedImage to LuminanceSrc", lumSrc);
    try {
      Result result = Receive.decodeSingle(lumSrc);
      assertEquals("Expect decoded result to match expected", expectedText, result.getText());
    } catch (NotFoundException e) {
      fail("Unable to find QR in image, "+filename + ". " + e.getMessage());
    }
  }

  /**
   * Decode single QR code that has had sequence information inserted into
   * its payload.
   */
  @Test
  public void testDecodeQrWithSequenceInfo() {
    // Screenshot of QR code received from transmitter
    String filename = "fooScreenshot_withReservedBits.png";
    String expectedText = "foo";

    // Decode the image
    Result result = decodeAndCheckValidQR(filename);
    PartialMessage m = PartialMessage.createFromResult(result, Integer.MAX_VALUE);

    // Expect payload to match 'expectedText' and only one QR code in sequence
    assertNotNull("Expected QR code to be formatted for QRLib", m);
    assertEquals("Should only have 1 chunk" , 1, m.getTotalChunks());
    assertEquals("Unexpected chunkId" , 1, m.getChunkId());
    String actualText = new String (m.getPayload(), Charsets.ISO_8859_1);
    assertEquals("Expect decoded result to match expected", expectedText, actualText);
  }

  @Test
  public void testEncodeThenDecode() throws IOException {
    Transmit t = new Transmit(350,350);
    byte[] inputBytes = "foo".getBytes(Charsets.ISO_8859_1);

    // Generate some QR codes from input string
    Iterable<BitmapImage> qrCodes = null;
    try {
      qrCodes = t.encodeQRCodes(inputBytes);
    } catch (TransmitException e) {
      fail("Encoding failed "+ e.getMessage());
    }
    Iterator<BitmapImage> iter = qrCodes.iterator();
    assertTrue("QR encoding failed to return at least one element", iter.hasNext());
    BitmapImage encodedQRImage = iter.next();

    // Convert them to images so we can run QR decoder on them
    BufferedImage b = UtilsTest.toBufferedImage(encodedQRImage);
    LuminanceSource lumSrc = new BufferedImageLuminanceSource(b);
    Result result = decodeAndCheckValidQR(lumSrc, null);
    PartialMessage m = PartialMessage.createFromResult(result, Integer.MAX_VALUE);

    // Expect this small input will generate and decode a single QR code.
    assertNotNull("Expected QR code to be formatted for QRLib", m);
    assertEquals("Should only have 1 chunk" , 1, m.getTotalChunks());
    assertEquals("Unexpected chunkId" , 1, m.getChunkId());
    assertArrayEquals("Original input does not match decoded result",
        inputBytes,m.getPayload());
  }

  /**
   * Returns result of QR code decode whenever the file, {@code filename},
   * contains a detectable QR code. Causes tests to fail if no QR code
   * could be detected.
   *
   * @param filename path to image file
   */
  private Result decodeAndCheckValidQR(String filename) {
    LuminanceSource img = getLuminanceImgAndCheckNotNull(filename);
    return decodeAndCheckValidQR(img, filename);
  }

  private Result decodeAndCheckValidQR(LuminanceSource img, String filename) {
    Result result = null;
    try {
      result = Receive.decodeSingle(img);
      assertNotNull("QR result should not be null", result);
    } catch (NotFoundException e) {
      if (filename != null) {
        fail("Unable to find QR in image, " + filename +". "+ e.getMessage());
      }else{
        fail("Unable to find QR in image. " + e.getMessage());
      }
    }
    return result;
  }
  private Iterable<Result> decodeMultipleAndCheckValidQR(LuminanceSource img, String filename) {
    Iterable<Result> results = null;
    try {
      results = Receive.decodeMultiple(img);

      assertNotNull("QR result should not be null", results);
      Iterator<Result> resIter = results.iterator();
      assertTrue("QR has at least one result", resIter.hasNext());
    } catch (NotFoundException e) {
      if (filename != null) {
        fail("Unable to find QR in image, " + filename +". "+ e.getMessage());
      }else{
        fail("Unable to find QR in image. " + e.getMessage());
      }
    }
    return results;
  }

  @Test
  public void testDecodeTwoQRCodesSameImage() throws ReceiveException {

    String expectAll = "qr_4Of4.png";

    // Use receive to decode this qr code:
    Receive receive = new Receive(0, 0, NULL_MONITOR);

    // The received data and track transmission status.
    DecodedMessage message = new DecodedMessage(NULL_MONITOR);

    LuminanceSource img = getLuminanceImgAndCheckNotNull(expectAll);
    Iterable <Result> results = decodeMultipleAndCheckValidQR(img,expectAll);
    State s = receive.saveMessageAndUpdateProgress(results, message);

    assertTrue("Expected to be in Final state.", s == State.Final);
    System.out.println("MESSAGE=" + message.toString());
  }

  /**
   * Opens a test file in resources directory and converts it to ZXing's
   * LuminanceSource image type. Causes unit tests to fail if conversion fails.
   *
   * @param filename The image file that will be converted.
   */
  private LuminanceSource getLuminanceImgAndCheckNotNull(String filename) {
    BufferedImage b = getImageResourceAndCheckNotNull(filename);
    LuminanceSource lumSrc = new BufferedImageLuminanceSource(b);
    assertNotNull("Unable to convert BufferedImage to LuminanceSrc", lumSrc);
    return lumSrc;
  }

  /**
   * Opens a test file in resources directory as BufferedImage. Causes unit
   * tests to fail if resource file cannot be opened or read. Can only use
   * in testing since Android does not have BufferedImage type.
   *
   * @param filename The image file that will be opened.
   */
  private BufferedImage getImageResourceAndCheckNotNull(String filename) {
    BufferedImage img = null;

    // Look for resource in classpath
    URL resource = ReceiveTest.class.getClassLoader().getResource(filename);
    assertNotNull("Expected resource to exist: " + filename, resource);
    try {
      img = ImageReader.readImage(resource.toURI());
      assertNotNull(img);
    } catch (URISyntaxException e) {
      fail("Malformed URL: " + resource.toString());
    } catch (IOException e) {
      fail("Cannot read resource file, " + filename + "." + e.getMessage());
    }
    return img;
  }

}