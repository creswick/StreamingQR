/**
 * Adapted from WriteYUV: https://code.google.com/p/java-yuv/source/browse/trunk/WriteYUV.java
 */
package com.galois.qrstream.qrpipe;

import java.awt.image.BufferedImage;

/**
 * @author creswick
 *
 */
public class YuvUtilities {

  private static final double PENGALI = 1.5;

  public static byte[] toYUV(BufferedImage bi) {
    int width = bi.getWidth();
    int height = bi.getHeight();

    
    
    byte[] frame = new byte[(int) (width * height * PENGALI)];

    boolean s = false;

    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        int color = bi.getRGB(i, j);

        int R = color >> 16 & 0xff;
        int G = color >> 8 & 0xff;
        int B = color & 0xff;

        int Y = (int) (R * .299000 + G * .587000 + B * 0.114000);
        int U = (int) (R * -.168736 + G * -.331264 + B * 0.500000 + 128);
        int V = (int) (R * .500000 + G * -.418688 + B * -0.081312 + 128);

        int arraySize = height * width;
        int yLoc = j * width + i;
        int uLoc = (j / 2) * (width / 2) + i / 2 + arraySize;
        int vLoc = (j / 2) * (width / 2) + i / 2 + arraySize + arraySize / 4;

        frame[yLoc] = (byte) Y;
        frame[uLoc] = (byte) U;
        frame[vLoc] = (byte) V;

        s = !s;
      }
    }

    return frame;
  }
}
