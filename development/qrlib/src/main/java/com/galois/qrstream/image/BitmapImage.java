/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.image;

import com.google.zxing.common.BitMatrix;


/**
 * Class used for converting from Zebra Xing library BitMatrix image type to
 * byte[] for Android to convert into Bitmap. This intermediate class is needed
 * since Android does not have BufferedImage in its Java implementation.
 *
 * Additionally, it communicates to application that is transmitting
 * the QR codes, the id of the QR code contained within the image
 * and the totalChunks being transmitted so that the
 * application doesn't have to decode the QR code to access this information.
 */
public class BitmapImage {

  private final BitMatrix m;
  private final int w;
  private final int h;
  private final int id;
  private final int total;

  /**
   * Convert BitMatrix to BitmapImage type
   * @param mat the BitMatrix to convert
   */
  public static BitmapImage createBitmapImage (int chunkId,
                                               int totalChunks,
                                               BitMatrix mat) {
    int w = mat.getWidth();
    int h = mat.getHeight();
    return new BitmapImage(chunkId,totalChunks,w,h,mat);
  }

  private BitmapImage(int chunkId, int totalChunks,
                      int width, int height,
                      BitMatrix bitMatrix) {
    id = chunkId;
    total = totalChunks;
    w = width;
    h = height;
    m = bitMatrix;
  }

  public int getWidth() {
    return w;
  }

  public int getHeight() {
    return h;
  }

  @Override
  public String toString() {
    String label = "chunk " + id;
    if (total > 1) {
        return label + " of " + total;
    }
    return label;
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
    return m.get(x, y);
  }

  /**
   * BitMatrix accessor, currently only used for testing, which is why it is
   * protected.
   */
  protected BitMatrix getBitMatrix() {
    return m;
  }
}
