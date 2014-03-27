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
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.galois.qrstream.image.BitmapImage;
import com.google.common.base.Charsets;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;

public class ReceiveTest {

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

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
    } catch (ReceiveException e) {
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
    PartialMessage m = PartialMessage.createFromResult(result);

    // Expect payload to match 'expectedText' and only one QR code in sequence
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
    PartialMessage m = PartialMessage.createFromResult(result);

    // Expect this small input will generate and decode a single QR code.
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
    } catch (ReceiveException e) {
      if (filename != null) {
        fail("Unable to find QR in image, " + filename +". "+ e.getMessage());
      }else{
        fail("Unable to find QR in image. " + e.getMessage());
      }
    }
    return result;
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

  /**
   * Print the payload of the decoded QR code to stdout.
   * @param result The {@code Result} of decoding image with QR code
   */
  private void printQrPayload(Result result) {
    // Get the payload from the decoded results and print the result
    System.out.print("payload from QR code before extracting reserved bits: ");
    @SuppressWarnings("unchecked")
    List<byte[]> payloadSegments =
        (List<byte[]>) result.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
    for (byte[] s : payloadSegments) {
      System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(s));
    }
  }

}