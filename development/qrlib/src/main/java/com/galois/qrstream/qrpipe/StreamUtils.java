package com.galois.qrstream.qrpipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

  public static String readString(InputStream in, int len) throws IOException {
    byte[] buf = new byte[len];
    in.read(buf);
    return new String(buf);
  }
  
  public static void writeInt(OutputStream out, int length) throws IllegalArgumentException, IOException {
    out.write(Utils.intToBytes(length));
  }
  
  public static void writeShort(OutputStream out, short s) throws IllegalArgumentException, IOException {
    out.write(Utils.shortToBytes(s));
  }
  
  public static int readInt(InputStream in) throws IOException {
    byte[] buf = new byte[Integer.SIZE / 8];
    in.read(buf);
    int len = Utils.bytesToInt(buf);
    return len;
  }
  
  public static short readShort(InputStream in) throws IOException {
    byte[] buf = new byte[Short.SIZE / 8];
    in.read(buf);
    short len = Utils.bytesToShort(buf);
    return len;
  }
}
