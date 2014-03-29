package com.galois.qrstream.qrpipe;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.galois.qrstream.image.BitmapImage;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

/**
 * Class provides API for interfacing with Android application. It
 * facilitates transmission of QR codes in streaming QR code protocol.
 *
 * We may find it necessary to add context to the transmission. For example,
 *  - resulting image dimension (px)
 *  - density of QR code (QR version 1-40, not currently exposed in API)
 *  - others?
 */
public class Transmit {

  /* Dimension of transmitted QR code images */
  private final int imgHeight;
  private final int imgWidth;

  public Transmit(int height, int width) {
    imgHeight = height;
    imgWidth = width;
  }

  /**
   * Encodes array of bytes into a collection of QR codes. It is designed to
   * interface with QRStream Android application. It will break input data into
   * chunks small enough for encoding into QR codes.
   *
   * @param data The array of bytes to encode
   * @return The sequence of QR codes generated from input data.
   * @throws TransmitException if input {@code data} cannot be encoded as QR code.
   */
  // TODO Step 2: change function to: public Iterable<BitmapImage> encodeQRCodes (Iterable<byte[]>)
  public Iterable<BitmapImage> encodeQRCodes(final byte[] data) throws TransmitException {
    // TODO: Settle on appropriate density and error level for phones.
    // Assume particular QR density and error correction level so that
    // we can calculate the appropriate chunk size for the input data.
    Version qrVersion = Version.getVersionForNumber(1);
    ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;
    return encodeQRCodes(data, qrVersion, ecLevel);
  }

  /**
   * Encodes array of bytes into a collection of QR codes. It will break input
   * into chunks small enough for encoding into QR codes for the requested
   * QR code density and error correction level.
   *
   * @param data The array of bytes to encode
   * @param qrVersion The desired density of the resulting QR code (i.e. version 1-40).
   * @param ecLevel Error correction level, can be either {@code L, M, Q, H}.
   * @return The sequence of QR codes generated from input data.
   * @throws TransmitException if input {@code data} cannot be encoded as QR code.
   */
  protected Iterable<BitmapImage> encodeQRCodes(final byte[] data,
      final Version qrVersion,
      final ErrorCorrectionLevel ecLevel) throws TransmitException {

    if (data == null || data.length <= 0) {
      return Lists.newArrayList();
    }

    // Check that image dimensions specified are large enough
    // to display the QR code generated with the requested density.
    if (Math.min(imgWidth, imgHeight) < qrVersion.getDimensionForVersion()) {
      throw new TransmitException("Requested image dimensions too small for "
          + "QR version " + qrVersion.getVersionNumber()
          + "Expected at least " + qrVersion.getDimensionForVersion()
          + ", but got (" + imgWidth + "," + imgHeight + ").");
    }
    
    return new Iterable<BitmapImage>() {
      @Override
      public Iterator<BitmapImage> iterator() {
        return new ImgIterator(data, qrVersion, ecLevel);
      }
    };
  }
  
  /**
   * Iterator to generate QR code bitmaps on demand.
   * @author creswick
   *
   */
  private class ImgIterator implements Iterator<BitmapImage> {

    private final Version qrVersion;
    private final ErrorCorrectionLevel ecLevel;
    
    private final int maxChunkSize;
    private final int totalChunks;
    
    /**
     * The current chunk id, 1-indexed. (The first chunk has chunkId = 1.)
     */
    private int chunkId;
    private ByteArrayInputStream byteInputStream;

    public ImgIterator(byte[] data, Version qrVersion,
        ErrorCorrectionLevel ecLevel) {
      this.qrVersion = qrVersion;
      this.ecLevel = ecLevel;
      
      maxChunkSize = getPayloadMaxBytes(ecLevel, qrVersion);
      totalChunks = getTotalChunks(data.length, maxChunkSize);
      chunkId = 0;
      
      byteInputStream = new ByteArrayInputStream(data);
    }

    @Override
    public boolean hasNext() {
      return chunkId != totalChunks;
    }

    @Override
    public BitmapImage next() {
      int bytesRemaining = byteInputStream.available();

      byte[] dataChunk = null;
      if (bytesRemaining <= maxChunkSize) {
        dataChunk = new byte[bytesRemaining];
      } else {
        dataChunk = new byte[maxChunkSize];
      }
      int bytesRead = byteInputStream.read(dataChunk, 0, dataChunk.length);
      if (bytesRead < 0) {
        throw new AssertionError(
            "Data reportedly available to read but read returned end of input.");
      } else if (bytesRead != dataChunk.length) {
        throw new AssertionError("Should be possible to read "
            + dataChunk.length + " bytes when there are " + bytesRemaining
            + " bytes available.");
      }

      chunkId++;
      return encodeQRCode(dataChunk, chunkId, totalChunks, qrVersion, ecLevel);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing QR codes is not supported.");
    }

  }

  /**
   * Generates a QR code for the input bytes, {@code chunkedData}. It is
   * assumed that {@code chunkedData} is subset of larger input. This function
   * will concatenate the {@code chunkId} and {@code totalChunks} that input
   * was divided into, to {@code chunkedData} before encoding as QR code.
   *
   * @param chunkedData A portion of input bytes to encode as QR code
   * @param chunkId An integer identifying {@code chunkedData} in sequence.
   * @param totalChunks The number of chunks the original input divided into.
   * @param v The desired density of the resulting QR code (i.e. version 1-40).
   * @param ecLevel Error correction level, can be either {@code L, M, Q, H}.
   * @return
   */
  protected BitmapImage encodeQRCode(byte[] chunkedData, int chunkId, int totalChunks,
                                     Version v, ErrorCorrectionLevel ecLevel) {
    if (chunkedData == null) {
      throw new NullPointerException("Cannot encode 'null' value as QR code.");
    }
    if (chunkedData.length > getPayloadMaxBytes(ecLevel, v)) {
      throw new IllegalArgumentException(
          "Input data too large for QR version: " + v.getVersionNumber()
              + " and error correction level: " + ecLevel
              + " chunkedData.length = " + chunkedData.length + " maxPayload= "
              + getPayloadMaxBytes(ecLevel, v));
    }
    byte[] prependedData = Utils.prependChunkId(chunkedData, chunkId, totalChunks);
    BitMatrix bMat = bytesToQRCode(prependedData);
    return BitmapImage.createBitmapImage(bMat);
  }

  /**
   * Returns the maximum number of bytes that can be encoded in QR code with
   * version {@code v} and error correction level, {@code ecLevel}.
   *
   * @param ecLevel
   *          The error correction level, can be one of these values
   *          {@code L, M, Q, H}.
   * @param v
   *          The version corresponding to the density of the QR code, 1-40.
   * @return
   */
  protected int getPayloadMaxBytes(ErrorCorrectionLevel ecLevel, Version v) {

    // Max payload for (version,ecLevel) = number data bytes - header bytes
    Version.ECBlocks ecBlocks = v.getECBlocksForLevel(ecLevel);
    int numReservedBytes = Utils.getNumberQRHeaderBytes(v);
    int numDataBytes = v.getTotalCodewords() - ecBlocks.getTotalECCodewords();

    return numDataBytes - numReservedBytes;
  }

  /**
   * Generates a QR code given a set of input bytes. This function
   * assumes that any injection of a sequence number has already occurred.
   *
   * ZXing library only encodes String and does not accept byte[], therefore we
   *  1. Convert input byte[] to ISO-8859-1 encoded String
   *  2. Pass it to ZXing with the ISO-8859-1 character-set hint
   *  3. When ZXing sees ISO-8859-1 character-set they set the QR
   *     encoding mode to 'Byte encoding' and turn the String back to turn to byte[].
   * @param rawData the binary data to encode into a single QR code image.
   */
  protected BitMatrix bytesToQRCode(byte[] rawData) {
    /*
     * Max QR code density is function of error correction level and version of
     * QR code. Max bytes for QR in binary/byte mode = 2,953 bytes using, QR
     * code version 40 and error-correction level L.
     */
    String data = new String(rawData, Charsets.ISO_8859_1);
    /* NOTE: There is no significant advantage to using Alphanumeric mode rather than
     * Binary mode, in terms of the number of bits we can pack into a single QR code.
     * Assuming QR version 40 and error correction level L.
     *   A. Alphanumeric, max bits = 4296 max chars * 5.5 bits/char = 23,628.
     *   B. Binary/byte,  max bits = 2953 max chars * 8 bits/char = 23,624.
     */

    /*
     * ZXing will determine the necessary QR code density given the number
     * of input bytes. If number of bytes is greater than max QR code version,
     * ZXing will throw WriterException("Data too big").
     */
    QRCodeWriter writer = new QRCodeWriter();
    try {
      // note: writer.encode cannot return a null value, as written. (It would throw a NPE.)
      BitMatrix bMat = writer.encode(data, BarcodeFormat.QR_CODE,
          imgWidth, imgHeight, getEncodeHints());
      return bMat;
    } catch (WriterException e) {
      //throw new TransmitException(e.getMessage());
      // After looking through the ZXing source, I *think* this can only happen
      // due to:
      //  - sending too much data for the QR code (which we check)
      //  - setting a BarcodeFormat != QR_CODE
      //  - ZXing programmer error; WriterExceptions are used in the zxing source
      //    like assert(...).
      //
      // Given all that, I think it's safe to wrap this exception up as an
      // unchecked exception, and re-throw it.  If this happens, it will almost
      // certainly show up in the test suite, and it will let use Iterators.
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Returns properties for the ZXing QR code writer indicating use of
   * ISO-8859-1 character set and the desired error correction level.
   *
   * There are four error acceptable error correction levels:
   *   Level L (Low)        7% of codewords can be restored.
   *   Level M (Medium)    15% of codewords can be restored.
   *   Level Q (Quartile)  25% of codewords can be restored.
   *   Level H (High)      30% of codewords can be restored.
   */
  private Map<EncodeHintType, Object> getEncodeHints(ErrorCorrectionLevel errorLevel) {
    /* Hints */
    HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
    hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
    hints.put(EncodeHintType.ERROR_CORRECTION, errorLevel);

    return hints;
  }

  /**
   * Returns the ZXing QR code writer properties indicating use of
   * ISO-8859-1 character set and a low level of error correction.
   */
  private Map<EncodeHintType, Object> getEncodeHints() {
    return getEncodeHints(ErrorCorrectionLevel.L);
  }

  /**
   * Returns the number of {@code desiredChunkSize} servings we can fit
   * into {@code length}. If {@code desiredChunksSize} does not fit
   * evenly, it rounds up to the nearest integer.
   *
   * @param length the input size that we want to split into chunks.
   * @param desiredChunkSize the size of split that we want to break {@source length} into.
   */
  private int getTotalChunks(int length, int desiredChunkSize) {
    if (length < 1 || desiredChunkSize < 1) {
      throw new IllegalArgumentException("Input must be positive.");
    }
    return ((int) Math.ceil((float)length/desiredChunkSize));
  }

}//public class Transmit
