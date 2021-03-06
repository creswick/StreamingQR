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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.galois.qrstream.image.BitmapImage;
import com.google.common.io.Files;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

public class UtilsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testBytesToIntConversions() {
    // Check that we padded smaller inputs correctly
    try {
      assertEquals("Expect 0 = 0x00", 0,
          Utils.bytesToInt(new byte[] { 0x00 }));
      assertEquals("Expect 1 = 0x01", 1,
          Utils.bytesToInt(new byte[] { 0x01 }));
      assertEquals("Expect 10,000 = 0x2710", 10000,
          Utils.bytesToInt(new byte[] { 0x27, 0x10 }));
      assertEquals("Expect 98,048 = 0x017f00", 98048,
          Utils.bytesToInt(new byte[] { 0x01, 0x7f, 0x00 }));
      assertEquals("Expect 655,360,018 = 0x27100012", 655360018,
          Utils.bytesToInt(new byte[] { 0x27, 0x10, 0x00, 0x12 }));
    } catch (IllegalArgumentException e){
      System.out.println("Do not fail test when IllegalArgumentException "+
          "thrown. In this case, it will only occur when NUM_BYTES_PER_INT < 4");
    }
  }

  @Test
  public void testBytesToIntRoundTripConversion() {
    byte[] a = new byte[] {0x27,0x10,0x00,0x12};
    int b = 655360018;

    assertEquals("Expect 1 = bytesToInt(intToBytes(1))", 1,
        Utils.bytesToInt(Utils.intToBytes(1)));
    try {
      assertEquals("Expect x = bytesToInt(intToBytes(x))", b,
          Utils.bytesToInt(Utils.intToBytes(b)));
      assertArrayEquals("Expect x = intToBytes(BytesToInt(x))", a,
          Utils.intToBytes(Utils.bytesToInt(a)));

      byte[] bytesMax = Utils.intToBytes(Integer.MAX_VALUE);
      assertEquals("Expect Integer.MAX_VALUE = bytesToInt(intToBytes(MAX_VALUE))",
          Integer.MAX_VALUE, Utils.bytesToInt(bytesMax));
    } catch (IllegalArgumentException e){
      System.out.println("Do not fail test when IllegalArgumentException "+
          "thrown. In this case, it will only occur when NUM_BYTES_PER_INT < 4");
    }
  }

  @Test
  public void testBytesToIntWithNegativeInput() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot convert negative numbers.");
    // Negative integers are not converted
    byte[] negOne = new byte[] { (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff };
    Utils.bytesToInt(negOne);
  }

  @Test
  public void testBytesToIntWithInputTooLarge() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Byte array too large.");
    Utils.bytesToInt(new byte[] { 0x01, 0x01, 0x01, 0x01, 0x01});
  }

  @Test
  public void testIntToBytesConversion() {
    // Check positive ints convert to byte[]
    assertArrayEquals("Expect 0 = 0x00000000",
        new byte[] {0x00,0x00,0x00,0x00}, Utils.intToBytes(0));
    assertArrayEquals("Expect 98,048 = 0x00017f00",
        new byte[] { 0x00, 0x01, 0x7f, 0x00 },
        Utils.intToBytes(98048));
  }

  @Test
  public void testIntToBytesWithNegativeInput() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot convert negative numbers.");
    // Negative integers are not converted
    Utils.intToBytes(-1);
  }

  @Ignore("Enable if NUM_BYTES_PER_INT < 4") @Test
  public void testIntToBytesWithInputTooLarge() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Byte array too large.");
    Utils.intToBytes(Integer.MAX_VALUE);
  }

  /**
   * Reads image from file and returns its BitMatrix. This function
   * will return null whenever the resource file cannot be found or read.
   *
   * @param resourceName
   * @return
   * @throws IOException if resourceName cannot be opened.
   * @throws NotFoundException
   */
  public static BitMatrix readQRImage (String resourceName) throws IOException {
    BitMatrix img = null;

    // Look for resource in classpath
    URL resource = UtilsTest.class.getClassLoader().getResource(resourceName);
    if (resource != null) {
      try {
        // Convert BufferedImage to BitMatrix
        img = toBitMatrix(ImageReader.readImage(resource.toURI()));
      } catch (URISyntaxException e) {
        throw new AssertionError("Malformed URL: " + resource.toString());
      }
    }
    return img;
  }

  /**
   * Returns bytes read from requested resource file.
   * @param filename Name of file in resources directory.
   * @return The byte array read from the {@source filename}.
   */
  protected static byte[] getTextResourceAndCheckNotNull(String filename) {
    byte[] result = null;

    // Look for resource in classpath
    URL resource = TransmitTest.class.getClassLoader().getResource(filename);
    assertNotNull("Expected resource to exist: " + filename, resource);

    try {
      result = Files.toByteArray(new File(resource.toURI()));
    } catch (URISyntaxException e) {
      fail("Malformed URL: " + resource.toString());
    } catch (IOException e) {
      fail("Cannot read resource file, " + filename + "." + e.getMessage());
    }
    assertNotNull(result);
    return result;
  }

  /**
   * Testing utility that converts BitmapImage to BufferedImage type.
   */
  protected static BufferedImage toBufferedImage(BitmapImage matrix) {
    MatrixToImageConfig config = new MatrixToImageConfig();
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    int onColor = config.getPixelOnColor();
    int offColor = config.getPixelOffColor();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
      }
    }
    return image;
  }

  /**
   * Convert from Java's BufferedImage type to ZXing's BitMatrix type.
   * Returns null when there is no QR code found in the image.
   *
   * @param img The BufferedImage to convert to BitMatrix
   * @return The BitMatrix of the QR code found in img
   */
  private static BitMatrix toBitMatrix (BufferedImage img){
    BufferedImageLuminanceSource lumSrc = new BufferedImageLuminanceSource(img);
    HybridBinarizer hb = new HybridBinarizer(lumSrc);
    try {
      return hb.getBlackMatrix();
    } catch (NotFoundException e) {
      // Ok to ignore, returning null when QR code not found.
      // PMD complained about empty catch block
      return null;
    }
  }
}
