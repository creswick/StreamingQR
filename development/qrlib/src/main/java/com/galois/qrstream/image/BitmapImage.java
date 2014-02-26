package com.galois.qrstream.image;


/**
 * Class used for converting from Zebra Xing library BitMatrix image type to
 * byte[] for Android to consume and convert into Bitmap.
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
