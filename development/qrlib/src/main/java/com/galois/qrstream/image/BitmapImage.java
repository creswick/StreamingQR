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

  public BitmapImage(int width, int height) {
    w = width;
    h = height;
    data = new byte[w * h];
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

  /**
   * Sets the given bit to true, where true means black.
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void set(int x, int y) throws IllegalArgumentException {
    if (x < 0 || y < 0 || x > w || y > h) {
      throw new IllegalArgumentException("Trying to set bit that is out of bounds.");
    }
    int offset = x + y * w;
    data[offset] = 0x01;
  }

  /**
   * Returns true if the given bit is set, where true means black.
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public boolean get(int x, int y) throws IllegalArgumentException {
    if (x < 0 || y < 0 || x > w || y > h) {
      throw new IllegalArgumentException("Trying to get bit that is out of bounds.");
    }
    int offset = x + y * w;
    return (data[offset] != 0);
  }
}
