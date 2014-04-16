package com.galois.qrstream.image;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

import com.galois.qrstream.qrpipe.YuvUtilities;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
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
   * Convert the output of Transmit.encodeQRCodes(...) into the input for Receive.decodeQRCodes(...).
   * 
   * @param codes
   * @return
   */
  public static BlockingQueue<YuvImage> toYuvQueue(Iterable<BitmapImage> codes) {

    Iterable<YuvImage> yImgs = Iterables.transform(codes,
        new Function<BitmapImage,YuvImage>(){
      @Override
      public YuvImage apply(BitmapImage input) {
        byte[] buff = YuvUtilities.toYUV(ImageUtils.toBufferedImage(input));
        return new YuvImage(buff, input.getWidth(), input.getHeight());
      }
    });
    return Queues.newLinkedBlockingQueue(yImgs);
  }
}
