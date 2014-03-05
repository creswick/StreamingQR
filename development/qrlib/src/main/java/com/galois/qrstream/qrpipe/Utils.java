package com.galois.qrstream.qrpipe;

import java.nio.ByteBuffer;

/**
 * This class provides constants and utility functions
 * common between Receive and Transmit Classes.
 */
public class Utils {
  /* There can be at most 4 bytes to represent an integer */
  private static final int MAX_INT_SIZE = 4;

  /* Number of bytes per integer to reserve at the front of QR code
   * payload. They will be used to keep track of the total QR codes
   * encoded as well as the id of the current QR code.
   * Must be <= MAX_INT_SIZE.
   */
  private static final int NUM_BYTES_PER_INT = 4;

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
      // Suppress dead-code warning since someone could change
      // NUM_BYTES_PER_INT to be less than MAX_INT_SIZE.
      throw new IllegalArgumentException("Byte array too large");
    }
    ByteBuffer bb = ByteBuffer.allocate(MAX_INT_SIZE).putInt(i);

    byte[] result = new byte[NUM_BYTES_PER_INT];
    if (bb.hasArray()) {
      bb.rewind();
      bb.get(result, MAX_INT_SIZE - NUM_BYTES_PER_INT, MAX_INT_SIZE);
    }
    return result;
  }

}
