package com.galois.qrstream.qrpipe;

import com.galois.qrstream.image.BitmapImage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;

/**
 * Class provides API for interfacing with Android application. It
 * facilitates transmission of QR codes in streaming QR code protocol.
 *
 * We may find it necessary to add context to the transmission. For example,
 *  - resulting image dimension (px) (DONE)
 *  - density of QR code
 *  - others?
 */
public class Transmit {

  /* Dimension of transmitted QR code images */
  private final int imgHeight;
  private final int imgWidth;

  public Transmit(int height, int width) {
    imgHeight = height;
    imgWidth = width;
  }
  
  public int getHeight() {
    return imgHeight;
  }

  public int getWidth() {
    return imgWidth;
  }

  /**
   * Encodes array of bytes into a collection of QR codes.
   * Designed to interface with QRStream Android application.
   *
   * @param data The array of bytes to encode
   * @return The sequence of QR codes generated from input data.
   */
  public Iterable<BitmapImage> encodeQRCodes (byte[] data) {
    // The collection of qr codes containing encoded data
    ArrayList<BitmapImage> qrCodes = new ArrayList<BitmapImage>();
    return qrCodes;

    // TODO Step 2: change function to: public Iterable<byte[]> encodeQRCodes (Iterable<byte[]>)
    //
    // The image width and height provided in constructor.
  }

  // TODO Add hints to change QR code error correction level or character set
  //      Other changes to QRCode parameters will need to be done by
  //      subclassing QRCodeWriter and overwriting encode method.  That
  //      would expose the QRCode element and allow us to make other changes.
  protected static BitMatrix stringToQRCode(String data, int width, int height) throws WriterException {
    QRCodeWriter writer = new QRCodeWriter();
    return writer.encode(data, BarcodeFormat.QR_CODE, width, height);
  }


}
