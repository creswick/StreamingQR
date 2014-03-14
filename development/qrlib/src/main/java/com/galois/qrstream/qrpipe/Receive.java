package com.galois.qrstream.qrpipe;

import com.galois.qrstream.image.YuvImage;
import java.util.concurrent.BlockingQueue;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.galois.qrstream.image.YuvImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

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
  public byte[] decodeQRCodes (BlockingQueue<YuvImage> qrCodeImages) {
    System.out.println("decodeQRcodes STARTED");
    progress.changeState(state);
    int count = 0;
    try {
      while(true) {
        count += 1;
        qrCodeImages.take();
        System.out.println("decodeQRcodes Frame Taken "+count);
        if(count == 200) {
          state.set(0);
          progress.changeState(state);
        }
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

  // TODO Keep track of total frames decoded thus far with DecodeState.
  // Android will use this know when they've received all of the messages.

  // TODO Possibly add conversion between YUV image to Bitmap image
  // types (only for testing)

  
  protected byte[] decodeQRCode(byte[] yuvData) {
    return null;
  }

  /**
   * Returns properties for the ZXing barcode reader indicating use of
   * ISO-8859-1 character set and decoding of QR codes.
   */
  protected static Map<DecodeHintType, Object> getDecodeHints() {
    /* Hints */
    HashMap<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
    hints.put(DecodeHintType.CHARACTER_SET, "ISO-8859-1");
    // 
    Collection<BarcodeFormat> possibleFormats =
        new ArrayList<BarcodeFormat>(Collections.singletonList(BarcodeFormat.QR_CODE));
    hints.put(DecodeHintType.POSSIBLE_FORMATS, possibleFormats);
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

    return hints;
  }

  /**
   * Detects and decode QR code from a luminance image.
   * @param lumSrc The luminance image containing a QR code to decode.
   * @throws NotFoundException if there was problem detecting or decoding QR
   * code from image {@source lumSrc}.
   */
  public static Result decode(LuminanceSource lumSrc) throws NotFoundException {
    BinaryBitmap bmap = toBinaryBitmap(lumSrc);
    return new MultiFormatReader().decode(bmap, Receive.getDecodeHints());
  }

  /**
   * Convert luminance image to ZXing's BinaryBitmap type.
   * The ZXing decoders require input as BinaryBitmap.
   *
   * @param img The luminance image to convert to BinaryBitmap
   * @return The bitmap of the input image to be decoded by QR reader.
   */
  private static BinaryBitmap toBinaryBitmap (LuminanceSource lumSrc){
    HybridBinarizer hb = new HybridBinarizer(lumSrc);
    return (new BinaryBitmap(hb));
  }
}
