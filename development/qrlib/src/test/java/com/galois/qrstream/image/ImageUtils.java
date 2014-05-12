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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.galois.qrstream.qrpipe.YuvUtilities;
import com.google.common.base.Function;
import com.google.zxing.client.j2se.MatrixToImageWriter;

/**
 * Utilities for maniputaling image objects in QRLib
 * 
 * @author creswick
 *
 */
public class ImageUtils {
  
  /**
   * Generate a BufferedImage from a BitmapImage.
   * 
   * @param img The BitmapImage to convert.
   * @return A BufferedImage representing the QR code in the bitmap image.
   */
  public static BufferedImage toBufferedImage(BitmapImage img) {
    return MatrixToImageWriter.toBufferedImage(img.getBitMatrix());
  }

  /**
   * Combines two buffered images, aligning their centers.
   * 
   * Adapted from this StackOverflow answer:
   * http://stackoverflow.com/a/2319251/3446
   * 
   * @param a The "back" image.
   * @param front The "front" image.
   * @return A combination of the two images.
   */
  public static BufferedImage combineImages(BufferedImage back, BufferedImage front) {
    // create the new image, canvas size is the max. of both image sizes
    int w = Math.max(back.getWidth(), front.getWidth());
    int h = Math.max(back.getHeight(), front.getHeight());
    BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

    // paint both images, preserving the alpha channels
    Graphics g = combined.getGraphics();
    int backX = (w - back.getWidth()) / 2;
    int backY = (h - back.getHeight()) / 2;
    g.drawImage(back, backX, backY, null);
    
    int frontX = (w - front.getWidth()) / 2;
    int frontY = (h - front.getHeight()) / 2;
    g.drawImage(front, frontX, frontY, null);

    return combined;
  }
  
  /**
   * Create a rotated copy of an image.
   * 
   * @param img The image to be rotated
   * @param angle The angle in degrees
   * @return The rotated image
   */
  public static BufferedImage rotate(BufferedImage img, double angle) {
      double sin = Math.abs(Math.sin(Math.toRadians(angle))),
             cos = Math.abs(Math.cos(Math.toRadians(angle)));

      int w = img.getWidth(), h = img.getHeight();

      int neww = (int) Math.floor(w*cos + h*sin),
          newh = (int) Math.floor(h*cos + w*sin);

      BufferedImage bimg = new BufferedImage(neww, newh, img.getType());
      Graphics2D g = bimg.createGraphics();

      g.translate((neww-w)/2, (newh-h)/2);
      g.rotate(Math.toRadians(angle), w/2, h/2);
      g.drawRenderedImage(img, null);
      g.dispose();

      return bimg;
  }
  
  /**
   * Function type wrapper around YUV conversion.
   */
  public static Function<BitmapImage, YuvImage> toYuvImage =
      new Function<BitmapImage, YuvImage>() {
    @Override
    public YuvImage apply(BitmapImage input) {
      byte[] buff = YuvUtilities.toYUV(ImageUtils.toBufferedImage(input));
      return new YuvImage(buff, input.getWidth(), input.getHeight());
    }
  };

  /**
   * Function type wrapper around toBufferedImage 
   */
  public static Function<BitmapImage, BufferedImage> toBufferedImage =
      new Function<BitmapImage, BufferedImage>() {
    @Override
    public BufferedImage apply(BitmapImage img) {
      return toBufferedImage(img);
    }
  };

  public static Function<BufferedImage, BufferedImage> rotate(final double degrees){
      return new Function<BufferedImage, BufferedImage>() {
        @Override
        public BufferedImage apply(BufferedImage input) {
          return rotate(input, degrees);
        }
      };
  }
  
  /**
   * Function type wrapper around @code YuvUtilities.toYUV @code
   */
  public static Function<BufferedImage, YuvImage> buffToYuv =
      new Function<BufferedImage, YuvImage>() {

    @Override
    public YuvImage apply(BufferedImage input) {
      byte[] buff = YuvUtilities.toYUV(input);
      return new YuvImage(buff, input.getWidth(), input.getHeight());
    }
  };

  /**
   * Creates a Function that adds a background image to each argument when invoked.
   * 
   * @param background the background image to place each argument upon.
   * @return A Function that can be used to center images on a specified background.
   */
  public static Function<BufferedImage, BufferedImage> addBackground(
      final BufferedImage background) {
    return new Function<BufferedImage, BufferedImage>() {
      @Override
      public BufferedImage apply(BufferedImage foreground) {
        return combineImages(background, foreground);
      }
    };
  }
}
