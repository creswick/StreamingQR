package com.galois.qrstream.qrpipe;

import java.nio.ByteBuffer;

import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;

/**
 * This class provides constants and utility functions
 * common between Receive and Transmit Classes.
 */
public class Utils {
  /* Number of bytes per integer to reserve at the front of QR code
   * payload. They will be used to keep track of the total QR codes
   * encoded as well as the id of the current QR code.
   * Must be <= MAX_INT_SIZE.
   */
  private static final int NUM_BYTES_PER_INT = Integer.SIZE / 8;

  /* The number of integers we are reserving in QR payload for chunk data */
  private static final int NUM_RESERVED_INTS = 2;

  /* There can be at most 4 bytes to represent an integer */
  private static final int MAX_INT_SIZE = Integer.SIZE / 8;

  /* Transmitting and receiving of BYTE data */
  private static final Mode DATA_ENCODING = Mode.BYTE;

  /**
   * Converts big-endian byte array to integer. Expects input will
   * convert to non-negative integer value.
   *
   * @param b The array of bytes to convert to int.
   * @throws IllegalArgumentException if input bytes converts to negative number.
   * @throws IllegalArgumentException if length of input bytes is greater than
   * 32bit integer could hold
   * @throws IllegalArgumentException if length of input bytes is greater than
   * the reserved {@source NUM_BYTES_PER_INT} for integers in the QR code.
   */
  public static int bytesToInt (final byte[] b) throws IllegalArgumentException {
    int result = 0;

    if (b != null) {
      if (b.length > NUM_BYTES_PER_INT || b.length > MAX_INT_SIZE) {
        throw new IllegalArgumentException("Byte array too large.");
      }

      // Pad with zeros, as needed, to ensure we have 4 bytes to read integer
      ByteBuffer bb = ByteBuffer.allocate(NUM_BYTES_PER_INT);
      for (int i=b.length; i<MAX_INT_SIZE; i++) {
        bb.put((byte) 0);
      }
      bb.put(b, 0, b.length);
      result = bb.getInt(0);
    }
    if (result < 0) {
      throw new IllegalArgumentException("Cannot convert negative numbers.");
    }
    return result;
  }

  /**
   * Converts integer to big-endian byte array. The integer must be less than
   * max 2^*(8bits * {@code NUM_BYTES_PER_INT}) for conversion to work and
   * less than {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if integer is negative.
   * @throws IllegalArgumentException if integer would need more than
   * NUM_BYTES_PER_INT to represent correctly.
   */
  @SuppressWarnings("unused")
  public static byte[] intToBytes( final int i ) throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Cannot convert negative numbers.");
    }
    // When using fewer than 4 bytes to represent an integer, check
    // that converted integer will fit into expected byte array length.
    if (NUM_BYTES_PER_INT < MAX_INT_SIZE && (i > (2 ^ (8 * NUM_BYTES_PER_INT)))) {
      // We suppress dead-code warning since someone could change
      // NUM_BYTES_PER_INT to be less than MAX_INT_SIZE.
      throw new IllegalArgumentException("Byte array too large");
    }
    // Write bytes corresponding to integer, i, into buffer,
    // making sure to reset position before reading
    ByteBuffer bb = ByteBuffer.allocate(MAX_INT_SIZE).putInt(i);
    bb.rewind();

    byte[] result = new byte[NUM_BYTES_PER_INT];
    bb.get(result, MAX_INT_SIZE - NUM_BYTES_PER_INT, MAX_INT_SIZE);

    return result;
  }

  /**
   * Returns the number of bytes needed for QR header given
   * QR version {@code v}, 8-bit byte mode, and space reserved
   * for tracking sequence data in a series of data chunks.
   *
   * @param v The version of the QR code representing information density.
   */
  public static int getNumberQRHeaderBytes(Version v) {
    return Utils.getNumberOfReservedBytes() +
           ((DATA_ENCODING.getBits() +
             DATA_ENCODING.getCharacterCountBits(v) + 7) / 8);
  }

  /**
   * Returns the number of bits we're reserving in QR payload for
   * identifying chunk of data in a sequence of transmitted QR codes.
   */
  public static int getNumberOfReservedBytes() {
    return NUM_RESERVED_INTS * NUM_BYTES_PER_INT;
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
  public static byte[] prependChunkId(final byte[] rawData, int chunk, int totalChunks) {
    // Unable to prepend chunk number to rawData if receive invalid inputs
    if (totalChunks < 0 || chunk < 0) {
      throw new IllegalArgumentException("Number of chunks must be positive");
    }

    byte[] inputData = rawData == null ? new byte[0] : rawData.clone();
    // Reserve first NUM_BYTES_PER_INT bytes of data for chunk id and
    // another NUM_BYTES_PER_INT bytes of data for the totalChunks.
    byte[] chunkId = intToBytes(chunk);
    byte[] nChunks = intToBytes(totalChunks);
    byte[] combined = new byte[inputData.length + chunkId.length + nChunks.length];

    System.arraycopy(chunkId, 0, combined, 0, chunkId.length);
    System.arraycopy(nChunks, 0, combined, chunkId.length, nChunks.length);
    System.arraycopy(inputData, 0, combined, chunkId.length + nChunks.length, inputData.length);
    return combined;
  }

  /**
   * Returns the chunk id identifying the segment of data within an original
   * message that this {@code rawData} belongs to. Data is assumed to be at the
   * front of the input.
   *
   * @param rawData The input that we want to extract chunk id from.
   * @throws IllegalArgumentException if the length of {@code rawData} is less
   * than the bytes reserved for the chunkId and the total number of chunks.
   */
  public static int extractChunkId(final byte[] rawData) throws IllegalArgumentException {
    if (rawData == null || rawData.length < getNumberOfReservedBytes()) {
      throw new IllegalArgumentException("Input data is too small");
    }
    byte[] chunkId = new byte[MAX_INT_SIZE];
    System.arraycopy(rawData, 0, chunkId, 0, MAX_INT_SIZE);
    return bytesToInt(chunkId);
  }

  /**
   * Returns the total number of chunks that this portion of message was
   * originally broken into. Data is assumed to be at the front of the input.
   *
   * @param rawData The segment of input message containing the chunk total.
   * @throws IllegalArgumentException if the length of {@code rawData} is less
   * than the bytes reserved for the chunkId and the total number of chunks.
   */
  public static int extractTotalNumberChunks(final byte[] rawData) throws IllegalArgumentException {
    if (rawData == null || rawData.length < getNumberOfReservedBytes()) {
      throw new IllegalArgumentException("Input data is too small");
    }
    byte[] totalChunks = new byte[MAX_INT_SIZE];
    System.arraycopy(rawData, MAX_INT_SIZE, totalChunks, 0, MAX_INT_SIZE);
    return bytesToInt(totalChunks);
  }

  public static byte[] extractPayload(final byte[] rawData) {
    if (rawData == null || rawData.length <= getNumberOfReservedBytes()) {
      throw new IllegalArgumentException("Input data is too small");
    }
    byte[] payload = new byte[rawData.length - getNumberOfReservedBytes()];
    System.arraycopy(rawData, getNumberOfReservedBytes(),
                     payload, 0, payload.length);
    return payload;
  }

  public static short bytesToShort(byte[] buf) {
    ByteBuffer bbuf = ByteBuffer.allocate(Short.SIZE / 8);
    
    bbuf.put(buf);
    bbuf.flip();
    return bbuf.getShort();
  }

  public static byte[] shortToBytes(short s) {
    ByteBuffer bbuf = ByteBuffer.allocate(Short.SIZE / 8);
    
    bbuf.putShort(s);
    bbuf.flip();
    return bbuf.array();    
  }

}
