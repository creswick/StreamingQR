package com.galois.qrstream.image;

import java.util.Arrays;

/**
 * Class representing data from the YUV color space. It contains
 * luminance data, Y, followed by the (U=Cb and V=Cr) chroma
 * planes. According to Android, the chroma planes have half the width
 * and height of the luminance plane (4:2:0 subsampling).
 */
public class YuvImage {
  private final byte[] data;
  private final int w;
  private final int h;

  public YuvImage(byte[] yuvData, int width, int height) {
    w = width;
    h = height;
    if (yuvData == null) {
      data = new byte[0];
    } else {
      data = Arrays.copyOf(yuvData, yuvData.length);
    }
  }

  public int getWidth() {
    return w;
  }

  public int getHeight() {
    return h;
  }

  // To get luminance data, we could create ZXing's
  // PlanarYUVLuminanceSource object and call getMatrix().
  public byte[] getYuvData() {
    return data.clone();
  }
}

