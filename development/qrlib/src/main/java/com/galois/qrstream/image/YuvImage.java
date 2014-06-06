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

