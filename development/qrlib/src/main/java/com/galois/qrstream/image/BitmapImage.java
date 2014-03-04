package com.galois.qrstream.image;

import java.util.Arrays;


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
    w = width;
    h = height;
    if (imgData == null) {
      data = new byte[0];
    } else {
      data = Arrays.copyOf(imgData, imgData.length);
    }
  }
  
  public int getWidth() {
    return w;
  }

  public int getHeight() {
    return h;
  }
  
  public byte[] getData() {
    return data.clone();
  }

}
