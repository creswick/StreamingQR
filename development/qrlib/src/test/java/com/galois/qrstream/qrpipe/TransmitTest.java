package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

import com.galois.qrstream.image.BitmapImage;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;

public class TransmitTest {

  private final Transmit transmitter = new Transmit(350,350);

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  @Ignore("Not ready yet") @Test
  public void testEncodeQRCodes() {
    // TODO: Ensure that we encoded sequence of QR codes correctly
    fail("testEncodeQRCodes() not implemented yet.");
  }

  /**
   * Encoding of QR code with no data should yield no BitmapImage.
   */
  @Test
  public void testEncodeQRCodesNoData() {
    byte[] noByteData = new byte[0];

    Iterable<BitmapImage> qrCodes = transmitter.encodeQRCodes(noByteData);
    int size = 0;
    for(@SuppressWarnings("unused") BitmapImage c : qrCodes) {
       size++;
    }
    assertEquals("Empty transmission should yield no QR code", 0, size);
  }

  /**
   * Encoding string as QR code and comparing the result should be
   * equivalent the expected QR code read from an image file.
   */
  @Test
  public void testBytesToQRCode() {
    // The string to encode as a QR code
    String origStr = "foo"; //'foo' in hex: Ox666F6F

    // The bytes corresponding to origStr, it is used only to check
    // String -> byte[] conversion.
    byte[] expectedBytes = new byte[] { 0x66, 0x6F, 0x6F };

    /*
     *  Setup test string and make sure that we get
     *  back the expected byte[] given ISO-8859-1 encoding.
     */
    byte[] utfStr = stringToBytes(origStr);
    assertNotNull("Expected conversion to byte[] from String to be successful", utfStr);
    assertEquals(utfStr.length, expectedBytes.length);
    assertArrayEquals("", expectedBytes, utfStr);

    /*
     * Generate QR code from `origStr`
     */
    String filePrefix = "qr_" + origStr;
    BitMatrix qrActual = null;
    try {
      qrActual = transmitter.bytesToQRCode(utfStr);
      // Output file just to check QR scanner can read it
      System.out.println("wrote tmp file, " + transmitter.bitMatrixToTmpFile(qrActual,1,filePrefix));
    } catch (WriterException e) {
      fail("Failed to generate QR code: " + e.getMessage());
    } catch (IOException e) {
      fail("Failed to write qr code file, " + filePrefix + ":" + e.getMessage());
    }

    assertNotNull("Expected QR encoding of string,"+origStr+", to be successful", qrActual);

    /*
     * Read the expected QR code output from its resource file
     * and compare it to the BitMatrix from the generated QR code.
     */
    BitMatrix qrExpected = null;
    BitMatrix qrUnequal = null;
    try {
      qrExpected = readQRImage(filePrefix + ".png");
      qrUnequal = readQRImage("qr_anystringyouwant.png");
    } catch (IOException e) {
      fail("Failed to read expected QR code: " + e.getMessage());
    }

    assertNotNull("Expected to find QR code in image", qrExpected);
    assertNotNull("Expected to find QR code in image", qrUnequal);
    assertEquals ("QRcode from 'foo' must match", qrExpected, qrActual);
    assertNotEquals ("QRcode from 'foo' should not match QRcode from string, 'any string you want'", qrUnequal, qrActual);
  }

  /**
   * Encodes this {@code String} into a sequence of bytes using the
   * ISO-8859-1 charset, storing the result into a new byte array.
   *
   * @param origStr The string to encode with ISO-8859-1 charset
   * @return The resulting byte array
   * @throws AssertionError
   */
  private byte[] stringToBytes(String origStr) throws AssertionError {
    byte[] utfStr = new byte[0];
    try {
      utfStr = origStr.getBytes("ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      // ISO-8859-1 is supported, it would be unusual for ZXing library to fail here.
      throw new AssertionError("ISO-8859-1 character set encoding not supported");
    }
    return utfStr;
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
  private BitMatrix readQRImage (String resourceName) throws IOException {
    BitMatrix img = null;

    // Look for resource in classpath
    URL resource = TransmitTest.class.getClassLoader().getResource(resourceName);
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
   * Convert from Java's BufferedImage type to ZXing's BitMatrix type.
   * Returns null when there is no QR code found in the image.
   * 
   * @param img The BufferedImage to convert to BitMatrix
   * @return The BitMatrix of the QR code found in img
   */
  private BitMatrix toBitMatrix (BufferedImage img){

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
  
  @Test
  public void testBytesToIntConversions() {
    // Check that we padded smaller inputs correctly
    assertEquals("Expect 0 = 0x00", 0,
        Transmit.bytesToInt(new byte[] {0x00}));
    assertEquals("Expect 1 = 0x01", 1,
                 Transmit.bytesToInt(new byte[] {0x01}));
    assertEquals("Expect 10,000 = 0x2710", 10000,
                 Transmit.bytesToInt(new byte[] {0x27, 0x10}));
    assertEquals("Expect 98,048 = 0x017f00", 98048,
                 Transmit.bytesToInt(new byte[] {0x01,0x7f,0x00}));
    assertEquals("Expect 98,048 = 0x017f00", 655360018,
                 Transmit.bytesToInt(new byte[] {0x27,0x10,0x00,0x12}));
    
    // Check int converts to byte[]
    assertArrayEquals("Expect 0 = 0x00000000",
        new byte[] {0x00,0x00,0x00,0x00}, Transmit.intToBytes(0));

    // Check that round trip conversion works
    byte[] bytesMax = Transmit.intToBytes(Integer.MAX_VALUE);
    assertEquals("Expect Integer.MAX_VALUE = bytesToInt(intToBytes(MAX_VALUE))",
                 Integer.MAX_VALUE, Transmit.bytesToInt(bytesMax));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIntToBytesThrowsException() {
    byte[] negOne = new byte[] { (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff };
    System.out.println("Expect exception to occur" + Transmit.bytesToInt(negOne));
  }
}
