package com.galois.qrstream.qrpipe;

import com.galois.qrstream.image.YuvImage;
import java.util.concurrent.BlockingQueue;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
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
  private final int height;
  private final int width;

  /* Track progress of decoding */
  private final IProgress progress;

  public Receive(int height, int width, IProgress progress) {
    this.height = height;
    this.width = width;
    this.progress = progress;
  }

  /**
   * Detects and decodes QR codes found within a collection of
   * YUV images. Designed to interface with QRStream Android application.
   *
   * @param qrCodeImages The collection of YUV images to decode
   * @return The data decoded from collection of detected QR codes.
   */
  public byte[] decodeQRCodes (BlockingQueue<YuvImage> qrCodeImages) {
    System.out.println("decodeQRcodes: STARTED");
    /* Container for saving received data and tracking state */
    DecodedMessage message = new DecodedMessage(progress);
    int count = 0;
    try {
      while(true) {
        count += 1;
        // If we get exception during decoding, then we could not
        // read QR code contained in image. Try next one.
        YuvImage img = qrCodeImages.take();
        try {
          // TODO Try improving performance by spawning new thread run each image decoding
          Result res = decodeSingleQRCode(img.getYuvData());
          saveMessageAndUpdateProgress(res, message);
          System.out.println("decodeQRcodes: Decoded Frame "+count);
        } catch (ReceiveException e) {
          System.out.println("decodeQRcodes: Unable to read QR code in image, "+count);
          continue;
        }
      }
    } catch (InterruptedException e) {
    }
    System.out.println("decodeQRcodes: ENDED");
    return null;
  }

  /**
   * Detect and decode QR code from an image.
   * @param yuvData The YUV image data containing a single QR code.
   * @return The detected QR code {@code Result} type.
   * @throws ReceiveException If no QR code has been detected or decoding failed.
   */
  protected Result decodeSingleQRCode(byte[] yuvData) throws ReceiveException {
    // TODO: May need to change last parameter, reverseHorizontal,
    // depending on yuvData we get from Android.
    LuminanceSource src = new PlanarYUVLuminanceSource(yuvData,
        width, height, 0, 0, width, height, false);
    return decodeSingle(src);
  }

  /**
   * Identify the chunk of data decoded from the QR code and
   * add its message to the collection of already received chunks.
   *
   * @param decodedQR The result of decoding QR code from an image
   * @param receivedData The data decoded from prior images.
   */
  protected void saveMessageAndUpdateProgress(Result decodedQR, DecodedMessage receivedData) {
    int chunkId = getChunkId(decodedQR);
    int totalChunks = getTotalChunks(decodedQR);
    byte[] payload = getMessageChunk(decodedQR);
    receivedData.saveMessageChunk(chunkId, totalChunks, payload);
  }

  /**
   * Extracts the {@code chunkId} from the decoded QR code
   * indicating its position in data transmission.
   */
  protected static int getChunkId (final Result decodedQR) {
    byte[] message = getRawData(decodedQR);
    return Utils.extractChunkId(message);
  }

  /**
   * Extracts the total number of chunks in a sequence of
   * transmitted data from the decoded QR code.
   */
  protected static int getTotalChunks (final Result decodedQR) {
    byte[] message = getRawData(decodedQR);
    return Utils.extractTotalNumberChunks(message);
  }

  /**
   * Extracts the payload of a decoded QR code, ignoring any
   * sequence information from the transmission.
   */
  protected static byte[] getMessageChunk (final Result decodedQR) {
    byte[] message = getRawData(decodedQR);
    return Utils.extractPayload(message);
  }

  /**
   * Extract raw bytes from decoded QR code.
   * @throws AssertionError if ZXing library returned more than one array
   */
  protected static byte[] getRawData(final Result decodedQR) {
    byte[] rawBytes = new byte[0];

    @SuppressWarnings("unchecked")
    List<byte[]> dataSegments = (List<byte[]>) decodedQR.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
    if (!dataSegments.isEmpty()) {
      // I'm not sure why dataSegments would have more than one entry.
      if (dataSegments.size() > 1) {
        System.out.println("Decoded result has "+dataSegments.size()+" elements. We expected just one.");
        throw new AssertionError();
      }
      rawBytes = dataSegments.get(0);
    }
    return rawBytes;
  }

  /**
   * Returns properties for the ZXing barcode reader indicating use of
   * ISO-8859-1 character set and decoding of QR codes.
   */
  protected static Map<DecodeHintType, Object> getDecodeHints() {
    /* Hints */
    HashMap<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
    hints.put(DecodeHintType.CHARACTER_SET, "ISO-8859-1");

    Collection<BarcodeFormat> possibleFormats =
        new ArrayList<BarcodeFormat>(Collections.singletonList(BarcodeFormat.QR_CODE));
    hints.put(DecodeHintType.POSSIBLE_FORMATS, possibleFormats);
    //TODO: if running to slow then try removing this hint.
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

    return hints;
  }

  /**
   * Detects and decode QR code from a luminance image.
   * @param lumSrc The luminance image containing a QR code to decode.
   * @throws ReceiveException if there was problem detecting or decoding QR
   * code from image {@source lumSrc}.
   */
  protected static Result decodeSingle(LuminanceSource lumSrc) throws ReceiveException {
    return decodeSingle(lumSrc, Receive.getDecodeHints());
  }

  /**
   * Detects and decode QR code from a luminance image.
   * @param lumSrc The luminance image containing a QR code to decode.
   * @param hints Hints to help the ZXing barcode reader find QR code easier
   * @throws ReceiveException if there was problem detecting or decoding QR
   * code from image {@source lumSrc}.
   */
  protected static Result decodeSingle(LuminanceSource lumSrc,
                                       Map<DecodeHintType,?> hints) throws ReceiveException {
    BinaryBitmap bmap = toBinaryBitmap(lumSrc);
    try {
      return new MultiFormatReader().decode(bmap, hints);
    } catch (NotFoundException e) {
      throw new ReceiveException(e.getMessage());
    }
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
