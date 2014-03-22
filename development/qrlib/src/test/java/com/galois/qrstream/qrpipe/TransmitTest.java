package com.galois.qrstream.qrpipe;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.galois.qrstream.image.BitmapImage;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

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
    // Setup encoding parameters
    Version qrVersion = Version.getVersionForNumber(1);
    ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;

    byte[] noByteData = new byte[0];
    
    Iterable<BitmapImage> qrCodes =
        encodeQRAndCheckNotNull(noByteData, qrVersion, ecLevel);

    int size = 0;
    for(@SuppressWarnings("unused") BitmapImage c : qrCodes) {
       size++;
    }
    assertEquals("Empty transmission should yield no QR code", 0, size);
  }

  /**
   * Ensure that we encoded sequence of QR codes correctly
   * @throws IOException 
   * @throws WriterException 
   */
  @Test
  public void testEncodeQRCodes() throws TransmitException, IOException {

    // Input for test
    String filename = "random2kfile";
    byte[] expectedBytes = getTextResourceAndCheckNotNull(filename);

    testRoundTripWithPureBarcodeHint(expectedBytes);
    testRoundTripWithNoBarcodeHint(expectedBytes);
  }

  /**
   * Check that ZXing will round trip the input bytes when we send
   * the hint that resulting encoded image contains only a pure QR code.
   * @param expectedBytes the input byte array
   * @throws TransmitException
   */
  private void testRoundTripWithPureBarcodeHint(byte[] expectedBytes) throws TransmitException {
    // ZXing can incorrectly identify black blobs in image as finder
    // square and render it invalid code: https://code.google.com/p/zxing/issues/detail?id=1262
    // The suggestion was to tell ZXing when you just have QR code in image and nothing else.
    Map<DecodeHintType, Object> hints = Receive.getDecodeHints();
    hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);

    ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;
    int errorsDetectingQRCodes=0;
    for (int i=1; i<=40; i++) {
      Version qrVersion = Version.getVersionForNumber(i);
      errorsDetectingQRCodes += testRoundTrip(qrVersion, ecLevel, hints, expectedBytes);
    }
    assertEquals("Expect no problems detecting generated QR codes", 0, errorsDetectingQRCodes);
  }

  /**
   * Check that ZXing will round trip the input bytes when we do not send
   * the hint that resulting encoded image contains only a pure QR code.
   * @param expectedBytes the input byte array
   * @throws TransmitException
   */
  private void testRoundTripWithNoBarcodeHint(byte[] expectedBytes) throws TransmitException {
    // ZXing can incorrectly identify black blobs in image as finder
    // square and render it invalid code: https://code.google.com/p/zxing/issues/detail?id=1262
    // The suggestion was to tell ZXing when you just have QR code in image and nothing else.
    Map<DecodeHintType, Object> hints = Receive.getDecodeHints();

    ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;
    int errorsDetectingQRCodes=0;
    for (int i=26; i<=40; i++) {
      Version qrVersion = Version.getVersionForNumber(i);
      errorsDetectingQRCodes += testRoundTrip(qrVersion, ecLevel, hints, expectedBytes);
    }
    assertEquals("Expect no problems detecting generated QR codes", 0, errorsDetectingQRCodes);
  }

  /**
   * Check that some input, {@code expectedBytes}, can be encoded a sequence
   * of QR codes and then decoded and assembled into original data.
   * Note: This returns the number of QR codes that ZXing failed to detect
   *       at this QR density. That way calling function can iterate over all
   *       possible QR code versions before reporting failure.
   *       Test will fail when decoding fails.
   * @param qrVersion The density the QR codes should be
   * @param ecLevel The error correction level
   * @param hints The hints to the ZXing barcode reader to ease decoding.
   * @param expectedBytes The input to perform roundtrip check on.
   * @throws TransmitException 
   */
  private int testRoundTrip(Version qrVersion, ErrorCorrectionLevel ecLevel,
      Map<DecodeHintType, Object> hints, byte[] expectedBytes) throws TransmitException {

    // File prefix used for debugging failed QR detection
    String prefix="testRoundTrip_"+qrVersion.getVersionNumber()+"_"+ecLevel;
    if(hints.containsKey(DecodeHintType.PURE_BARCODE)) {
      prefix += "_PURE_BARCODE";
    }
    // Collect bytes that we decode, final result must match expectedBytes
    byte[] resultBytes = new byte[expectedBytes.length];

    // Count of failed QR code detections within sequence.
    int detectionErrors = 0;

    Iterable<BitmapImage> qrCodes = encodeQRAndCheckNotNull(expectedBytes, qrVersion, ecLevel);
    int size = 0;
    int resultLength = 0;
    for(BitmapImage c : qrCodes) {
      size++;
      BufferedImage b = UtilsTest.toBufferedImage(c);
      LuminanceSource lumSrc = new BufferedImageLuminanceSource(b);

      byte[] fromChunk = new byte[0];
      try {
        Result result = Receive.decodeSingle(lumSrc, hints);
        fromChunk = Receive.getMessageChunk(result);
      } catch (ReceiveException e) {
        try {
          detectionErrors++;
          System.out.print("Failed to read QR code (Version: "+
                           qrVersion+" chunk, "+size+"): ");
          System.out.println(bitmapImageToFile(c,size,prefix));
        } catch (IOException e1) {
          fail("Unable to write QR code to temporary file.");
        }
      }
      if (detectionErrors == 0) {
        assertTrue("Should have received some data but got none", fromChunk.length > 0);
        assertTrue("result length cannot be greater than original",
            (resultLength + fromChunk.length) <= expectedBytes.length);
        System.arraycopy(fromChunk, 0, resultBytes, resultLength, fromChunk.length);
        resultLength += fromChunk.length;
      }
    }
    if (detectionErrors == 0) {
      assertArrayEquals("Decoded result should be same as original msg",
        expectedBytes, resultBytes);
    }
    return detectionErrors;
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
    byte[] utfStr = origStr.getBytes(Charsets.ISO_8859_1);
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
    } catch (TransmitException e) {
      fail("Failed to generate QR code: " + e.getMessage());
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
   * Returns bytes read from requested resource file.
   * @param filename Name of file in resources directory.
   * @return The byte array read from the {@source filename}.
   */
  private byte[] getTextResourceAndCheckNotNull(String filename) {
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
   * Sets up a collection of QR codes generated from the input data. It
   * fails if there was some error in the initialization.
   * @param ecLevel The error correction level of the QR code
   * @param qrVersion The density of the QR code.
   * @throws WriterException if QR encoding fails to encode {@code data}. 
   */
  private Iterable<BitmapImage> encodeQRAndCheckNotNull(
      byte[] data, Version qrVersion, ErrorCorrectionLevel ecLevel)
          throws TransmitException {
    Iterable<BitmapImage> qrCodes = transmitter.encodeQRCodes(data, qrVersion, ecLevel);
    assertNotNull("Expect encoding will be successful", qrCodes);
    return qrCodes;
  }

  protected static String bitmapImageToFile(BitmapImage bitmap, int sequence,
                                   String filePrefix) throws IOException {

    File tmp = File.createTempFile(filePrefix + "_" + sequence +"_", ".png");
    ImageIO.write(UtilsTest.toBufferedImage(bitmap), "png", tmp);

    return tmp.getAbsolutePath();
  }
}
