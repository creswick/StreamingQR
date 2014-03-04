package com.galois.qrstream.image;


/**
 * Class used for converting from Zebra Xing library BitMatrix image type to
 * byte[] for Android to convert into Bitmap. This intermediate class is needed
 * since Android does not have BufferedImage in its Java implementation.
 */
public class BitmapImage {
  private final byte[] data;
  private final int w;
  private final int h;

  public BitmapImage(byte[] imgData, int width, int height) {
    data = imgData;
    w = width;
    h = height;
  }
  
  public int getWidth() {
    return w;
  }

  public int getHeight() {
    return h;
  }
  
  public byte[] getData() {
    return data;
  }

}
