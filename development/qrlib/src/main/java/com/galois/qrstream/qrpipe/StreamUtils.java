package com.galois.qrstream.qrpipe;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilities for pulling basic data types in/out of data streams.
 * 
 * 
 * @author creswick
 *
 */
public class StreamUtils {

  /**
   * Reads a string from an input stream using the default character encoding.
   *
   * @param in The input stream to read from.
   * @param len The length of the string, in bytes.
   * @return
   * @throws IOException If the underlying stream throws an exception.
   */
  public static String readString(InputStream in, int len) throws IOException {
    
    DataInputStream dis = new DataInputStream(in);
    byte[] buf = new byte[len];
    try {
      dis.readFully(buf);
    } catch (EOFException e) {
      // This may happen if the length coming in is incorrect -- we shouldn't
      // trust the data at this point:
      throw new IOException("Incorrect length in incoming data.", e);
    }
    return new String(buf);
  }
  
  /**
   * Write an int to a stream, as a sequence of bytes.
   * 
   * @see Utils.intToBytes
   * 
   * @param out
   * @param length
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static void writeInt(OutputStream out, int length) throws IllegalArgumentException, IOException {
    DataOutputStream dos = new DataOutputStream(out);
    dos.writeInt(length);
  }
  
  /**
   * Write a short to a stream as a sequence of bytes.
   * 
   * @see Utils.shortToBytes
   * 
   * @param out
   * @param s
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static void writeShort(OutputStream out, short s)
      throws IllegalArgumentException, IOException {
    DataOutputStream dos = new DataOutputStream(out);
    dos.writeShort(s);
  }
  
  /**
   * Read an int from a sequence of bytes.  Assumes that the encoding system 
   * uses the same number of bits as the decoding system.
   * 
   * @see Utils.bytesToInt
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static int readInt(InputStream in) throws IOException {
    DataInputStream dis = new DataInputStream(in);
    
    try {
      return dis.readInt();
    } catch (EOFException e) {
      // This may happen if the length coming in is invalid -- we shouldn't
      // trust the data at this point:
      throw new IOException("Insufficient data to read int.", e);
    }
  }
  
  /**
   * Read a short from a sequence of bytes.  Assumes that the encoding system 
   * uses the same number of bits as the decoding system.
   * 
   * @see Utils.bytesToShort
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static short readShort(InputStream in) throws IOException {
    DataInputStream dis = new DataInputStream(in);
    
    try {
      return dis.readShort();
    } catch (EOFException e) {
      // This may happen if the data coming in is invalid -- we shouldn't
      // trust the data at this point:
      throw new IOException("Insufficient data to read short.", e);
    }
  }
}
