package com.galois.qrstream.qrpipe;

import com.galois.qrstream.image.YuvImage;
import java.util.concurrent.ArrayBlockingQueue;
import java.lang.InterruptedException;

import java.lang.Iterable;

/**
 * Class provides API for interfacing with Android application. It
 * facilitates receipt of QR codes in streaming QR protocol.
 *
 * We may find it necessary to add context to the transmission. For example,
 *  - resulting image dimension (px) (DONE)
 *  - density of QR code
 *  - others?
 */
public class Receive {

  /* Dimension of received images */
  private int imgHeight;
  private int imgWidth;
  private IProgress progress;
  private DecodeState state;

  public Receive(int height, int width, IProgress progress) {
    imgHeight = height;
    imgWidth = width;
    this.progress = progress;
    this.state = new DecodeState(2);
  }
  
  public int getHeight() {
    return imgHeight;
  }
  
  public int getWidth() {
    return imgWidth;
  }

  /**
   * Detects and decodes QR codes found within a collection of
   * YUV images. Designed to interface with QRStream Android application.
   *
   * @param qrCodeImages The collection of YUV images to decode
   * @return The data decoded from collection of detected QR codes.
   */
  public byte[] decodeQRCodes (ArrayBlockingQueue<YuvImage> qrCodeImages) {
    System.out.println("decodeQRcodes STARTED");
    progress.changeState(state);
    try {
      while(true) {
        qrCodeImages.take();
        System.out.println("decodeQRcodes Frame Taken");
      }
    } catch (InterruptedException e) {
    }
    //throw new UnsupportedOperationException("Function not yet implemented.");
    // TODO Be sure to remove elements from Iterable collection after processing
    // TODO Step 2: Use Iterable interface, change definition to
    //      public Iterable<byte[]> decodeQRCodes(Iterable<YuvImage>)
    System.out.println("decodeQRcodes ENDED");
    return null;
  }

  // TODO Possibly add conversion between YUV image to Bitmap image
  // types (only for testing)

}
