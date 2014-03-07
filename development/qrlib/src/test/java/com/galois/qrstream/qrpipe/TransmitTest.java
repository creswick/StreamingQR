package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

import com.galois.qrstream.image.BitmapImage;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

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

  @Test
  public void testGetMaxDataEncoding() {
    // Expected values retrieved from QR standard, ISO/IEC 18004:2006
    int[] expectedMaxForLevelL = new int[] { 17, 32, 53, 78, 106, 134, 154,
        192, 230, 271, 321, 367, 425, 458, 520, 586, 644, 718, 792, 858, 929,
        1003, 1091, 1171, 1273, 1367, 1465, 1528, 1628, 1732, 1840, 1952, 2068,
        2188, 2303, 2431, 2563, 2699, 2809, 2953 };
    int[] expectedMaxForLevelH = new int[] { 7, 14, 24, 34, 44, 58, 64, 84, 98,
        119, 137, 155, 177, 194, 220, 250, 280, 310, 338, 382, 403, 439, 461,
        511, 535, 593, 625, 658, 698, 742, 790, 842, 898, 958, 983, 1051, 1093,
        1139, 1219, 1273 };
    int[] expectedMaxForLevelM = new int[] { 14, 26, 42, 62, 84, 106, 122, 152,
        180, 213, 251, 287, 331, 362, 412, 450, 504, 560, 624, 666, 711, 779,
        857, 911, 997, 1059, 1125, 1190, 1264, 1370, 1452, 1538, 1628, 1722,
        1809, 1911, 1989, 2099, 2213, 2331 };
    int[] expectedMaxForLevelQ = new int[] { 11, 20, 32, 46, 60, 74, 86, 108,
        130, 151, 177, 203, 241, 258, 292, 322, 364, 394, 442, 482, 509, 565,
        611, 661, 715, 751, 805, 868, 908, 982, 1030, 1112, 1168, 1228, 1283,
        1351, 1423, 1499, 1579, 1663 };

    compareMaxPayload(expectedMaxForLevelL, ErrorCorrectionLevel.L);
    compareMaxPayload(expectedMaxForLevelM, ErrorCorrectionLevel.M);
    compareMaxPayload(expectedMaxForLevelQ, ErrorCorrectionLevel.Q);
    compareMaxPayload(expectedMaxForLevelH, ErrorCorrectionLevel.H);
  }

  private void compareMaxPayload(int[] expectedMaxForLevel, ErrorCorrectionLevel ecLevel) {
    // We have to add the bytes that we set aside for tracking data chunks
    // in sequence before comparing the max to the standard.
    int reservedChunkSize = Utils.getNumberOfReservedBytes();
    for (int i = 1; i <= 40; i++) {
      assertEquals("Expected max bytes=" +expectedMaxForLevel[i-1] +
          " when QR version="+ i + ", errorLevel="+ecLevel,
          expectedMaxForLevel[i-1],
          transmitter.getPayloadMaxBytes(ecLevel, i) + reservedChunkSize);
    }
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
}
