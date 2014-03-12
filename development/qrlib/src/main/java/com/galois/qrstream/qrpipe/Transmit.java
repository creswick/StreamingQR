package com.galois.qrstream.qrpipe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.galois.qrstream.image.BitmapImage;
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
  public Iterable<BitmapImage> encodeQRCodes(final byte[] data) throws TransmitException {
    // The collection of qr codes containing encoded data
    ArrayList<BitmapImage> qrCodes = new ArrayList<BitmapImage>();

    if (data != null && data.length > 0) {
      // Assume particular QR density and error correction level so that
      // we can calculate the appropriate chunk size for the input data.
      Version qrVersion = Version.getVersionForNumber(40);
      ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;

      int maxChunkSize = getPayloadMaxBytes(ecLevel, qrVersion);
      int totalChunks = getTotalChunks(data.length, maxChunkSize);
      int chunkId = 0;

      ByteArrayInputStream input = null;
      try {
        input = new ByteArrayInputStream(data);
        int bytesRemaining;
        while ((bytesRemaining = input.available()) > 0) {
          byte[] dataChunk = null;
          if (bytesRemaining <= maxChunkSize) {
            dataChunk = new byte[bytesRemaining];
          } else {
            dataChunk = new byte[maxChunkSize];
          }
          int bytesRead = input.read(dataChunk, 0, dataChunk.length);
          if (bytesRead < 0) {
            throw new AssertionError(
                "Data reportedly available to read but read returned end of input.");
          } else if (bytesRead != dataChunk.length) {
            throw new AssertionError("Should be possible to read "
                + dataChunk.length + " bytes when there are " + bytesRemaining
                + " bytes available.");
          }
          chunkId += 1;
          qrCodes.add(encodeQRCode(dataChunk, chunkId, totalChunks,
                                   qrVersion, ecLevel));
        }
        if (chunkId != totalChunks) {
          throw new TransmitException("Failed to encode as many chunks as we "
              + "expected. Expected " + totalChunks + " but got " + chunkId);
        }
      } finally {
        // The close method for ByteArrayInputStream does
        // nothing but we have this here for completeness.
        // If using any other stream, remove try-catch around close.
        if (input != null) {
          try {
            input.close();
          } catch (IOException e) {
            throw new AssertionError(
                "ByteArrayInputStream never throws IOException in close()");
          }
        }
      }
    }
    return qrCodes;
  }


  protected BitmapImage encodeQRCode(byte[] chunkedData, int chunkId, int totalChunks,
                                     Version v, ErrorCorrectionLevel ecLevel) throws TransmitException {
    if (chunkedData == null) {
      throw new NullPointerException("Cannot encode 'null' value as QR code.");
    }
    if (chunkedData.length >= getPayloadMaxBytes(ecLevel, v)) {
      throw new IllegalArgumentException(
          "Input data too large for QR version: " + v.getVersionNumber()
              + " and error correction level: " + ecLevel
              + " chunkedData.length = " + chunkedData.length + " maxPayload= "
              + getPayloadMaxBytes(ecLevel, v));
    }
    BitMatrix bMat = bytesToQRCode(chunkedData);
        //prependChunkId(chunkedData, chunkId, totalChunks));
    if (bMat.getWidth() != imgWidth || bMat.getHeight() != imgHeight) {
      throw new AssertionError("Expected image dimensions to be equal");
    }
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
   * Injects chunk# and totalChunks into byte[] for encoding into QR code.
   * The first {@code NUM_BYTES_PER_INT} bytes are the chunk# followed by
   * {@code NUM_BYTES_PER_INT} bytes for the total # chunks.
   * 
   * Note:
   * Four bytes may be too much space to reserve, but it was convenient
   * to think about. We could probably just use 3 bytes for each int
   * and let MAX_INTEGER=2^24-1 = 16,777,215.
   * 
   * If 4 bytes, then max bytes transferred in indices alone would
   * equal 2,147,483,647 * 8 bytes = ~16GB.
   * If QR code could transfer ~1200 bytes, then largest transfer we could handle
   * is 2,147,483,647 * (1200 - 8 bytes) = ~2,384 GB.
   * Number realistic max chunks likely to be = 16 GB file / (1200 - 8) bytes
   *                                         ~= 14,412,642
   * Number realistic bits we'd need = log2(14,412,642) ~= 24
   */
  protected byte[] prependChunkId(byte[] rawData, int chunk, int totalChunks) {
    // Unable to prepend chunk number to rawData if receive invalid inputs
    if (totalChunks < 0 || chunk < 0) {
      throw new IllegalArgumentException("Number of chunks must be positive");
    }

    byte[] inputData = rawData == null ? new byte[0] : rawData.clone();
    // Reserve first NUM_BYTES_PER_INT bytes of data for chunk id and
    // another NUM_BYTES_PER_INT bytes of data for the totalChunks.
    byte[] chunkId = Utils.intToBytes(chunk);
    byte[] nChunks = Utils.intToBytes(totalChunks);
    byte[] combined = new byte[inputData.length + chunkId.length + nChunks.length];

    System.arraycopy(chunkId, 0, combined, 0, chunkId.length);
    System.arraycopy(nChunks, 0, combined, chunkId.length, nChunks.length);
    System.arraycopy(inputData, 0, combined, chunkId.length + nChunks.length, inputData.length);

    return combined;
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
  protected BitMatrix bytesToQRCode(byte[] rawData) throws TransmitException {
    /*
     * Max QR code density is function of error correction level and version of
     * QR code. Max bytes for QR in binary/byte mode = 2,953 bytes using, QR
     * code version 40 and error-correction level L.
     */
    String data;
    try {
      /* NOTE: There is no significant advantage to using Alphanumeric mode rather than
       * Binary mode, in terms of the number of bits we can pack into a single QR code.
       * Assuming QR version 40 and error correction level L.
       *   A. Alphanumeric, max bits = 4296 max chars * 5.5 bits/char = 23,628.
       *   B. Binary/byte,  max bits = 2953 max chars * 8 bits/char = 23,624.
       */
      data = new String(rawData, "ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("ISO-8859-1 character set encoding not supported");
    }
    /*
     * ZXing will determine the necessary QR code density given the number
     * of input bytes. If number of bytes is greater than max QR code version,
     * ZXing will throw WriterException("Data too big").
     */
    QRCodeWriter writer = new QRCodeWriter();
    try {
      return writer.encode(data, BarcodeFormat.QR_CODE, imgWidth, imgHeight,
          getEncodeHints());
    } catch (WriterException e) {
      throw new TransmitException(e.getMessage());
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
    if (length < 0 || desiredChunkSize < 0) {
      throw new IllegalArgumentException("Input must be non-negative.");
    }
    return (length + desiredChunkSize) / desiredChunkSize;
  }



}//public class Transmit
