package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

import com.galois.qrstream.image.BitmapImage;

import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TransmitTest {

  private final Transmit transmitter = new Transmit(350,350);

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  /**
   * Encoding of QR code with no data should yield no BitmapImage.
   * @throws WriterException
   */
  @Test
  public void testEncodeQRCodesNoData() throws TransmitException {
    byte[] noByteData = new byte[0];
    
    Iterable<BitmapImage> qrCodes = encodeQRAndCheckNotNull(noByteData);

    int size = 0;
    for(@SuppressWarnings("unused") BitmapImage c : qrCodes) {
       size++;
    }
    assertEquals("Empty transmission should yield no QR code", 0, size);
  }

  /**
   * TODO Ensure that we encoded sequence of QR codes correctly
   * @throws WriterException 
   */
  @Ignore("ignore until we add prepending of chunkId back into encode call") @Test
  public void testEncodeQRCodes() throws TransmitException {
    // The string to encode as a QR code
    String origStr = "foo";
    // The bytes corresponding to origStr
    byte[] expectedBytes = null;
    try {
      expectedBytes = origStr.getBytes("ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      fail("ISO-8859-1 character set encoding not supported");
    }

    //The expected QR code
    BitMatrix qrExpected = null;
    try {
      qrExpected = UtilsTest.readQRImage("photo.png");
    } catch (IOException e) {
      fail(e.getMessage());
    }
    assertNotNull(qrExpected);

    Iterable<BitmapImage> qrCodes = encodeQRAndCheckNotNull(expectedBytes);
    int size = 0;
    for(@SuppressWarnings("unused") BitmapImage c : qrCodes) {
       size++;
    }
    assertEquals("Expected one QR code", 1, size);
    BitmapImage qrActual = qrCodes.iterator().next();
    
    System.out.println("qrExpected: w: "+qrExpected.getWidth() + " h: "+qrExpected.getHeight());
    System.out.println("qrActual: w: "+qrActual.getWidth() + " h: "+qrActual.getHeight());
    assertArrayEquals ("BitmapImages equal?",
        Utils.toBitmapImage(qrExpected).getData(), qrActual.getData());
  }

  /**
   * Compare maximum QR code payload to maximums listed
   * in the QR standard, ISO/IEC 18004:2006.
   */
  @Test
  public void testGetMaxDataEncoding() {
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

  /**
   * Helper function that compares maximum QR code payload to 
   * maximums listed in the QR standard, ISO/IEC 18004:2006.
   */
  private void compareMaxPayload(int[] expectedMaxForLevel, ErrorCorrectionLevel ecLevel) {
    // We have to add the bytes that we set aside for tracking data chunks
    // in sequence before comparing the max to the standard.
    int reservedChunkSize = Utils.getNumberOfReservedBytes();
    for (int i = 1; i <= 40; i++) {
      assertEquals("Expected max bytes=" +expectedMaxForLevel[i-1] +
          " when QR version="+ i + ", errorLevel="+ecLevel,
          expectedMaxForLevel[i-1],
          transmitter.getPayloadMaxBytes(ecLevel, Version.getVersionForNumber(i)) + reservedChunkSize);
    }
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
      System.out.println("wrote tmp file, " + bitMatrixToTmpFile(qrActual,1,filePrefix));
    } catch (TransmitException e) {
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
      qrExpected = UtilsTest.readQRImage(filePrefix + ".png");
      qrUnequal = UtilsTest.readQRImage("qr_anystringyouwant.png");
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
   * Sets up a collection of QR codes generated from the input data. It
   * fails if there was some error in the initialization.
   * @throws WriterException if QR encoding fails to encode {@code data}. 
   */
  private Iterable<BitmapImage> encodeQRAndCheckNotNull(byte[] data) throws TransmitException{
    Iterable<BitmapImage> qrCodes = transmitter.encodeQRCodes(data);
    assertNotNull("Expect encoding will be successful", qrCodes);
    return qrCodes;
  }
  
  /**
   * Output a QR BitMatrix to png file in temporary directory for debugging
   * purposes. For example, a call to bitMatrixToTmpFile(m,12,"foo") will create
   * a temporary file, /tmp/foo_12.png.
   * 
   * @param m The bit matrix that will be written to file.
   * @param sequence Identifies the chunk of data that this QR code encodes.
   * @param filePrefix Name that identifies the output file.
   * @return The absolute path of the created temporary file.
   */
  private String bitMatrixToTmpFile(BitMatrix m, int sequence,
                                    String filePrefix) throws IOException {
    String imgType = "png";
    File tmp = File.createTempFile(filePrefix + "_" + sequence, "." + imgType);
    MatrixToImageWriter.writeToFile(m, imgType, tmp);

    return tmp.getAbsolutePath();
  }
}
