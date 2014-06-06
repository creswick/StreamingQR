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
package com.galois.qrstream.qrpipe;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.galois.qrstream.image.YuvImage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
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

  /* Useful to communicate no QR codes found */
  private static final Iterable<Result> NO_RESULTS = ImmutableList.of();

  /**
   * Initializes receiver of QR code stream.
   * @param height The height of the received images.
   * @param width The width of the received images.
   * @param progress The object used in tracking the progress of the message
   * transmission.
   */
  public Receive(int height, int width, IProgress progress) {
    this.height = height;
    this.width = width;
    this.progress = progress;
  }

  /**
   * Decode an object from an incoming stream of QR codes.
   *
   * @param qrCodeImages
   * @return
   * @throws ReceiveException
   */
  public Object decodeQRSerializable(IImageProvider frameManager)
     throws ReceiveException {
    byte[] buf = decodeQRCodes(frameManager);

    ByteArrayInputStream bais = new ByteArrayInputStream(buf);

    try {
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object res = ois.readObject();
      return res;
    } catch (Exception e) {
      // 'buf' will have zero length when qrlib was unable to decode
      // all qr codes or was canceled in the middle of transmission.
      // This causes ObjectInputStream to throw EOFException
      // in 'readObject'.
      // TODO Be consistent with how receiver handles failed transmission/decoding.
      // Either always throw exception, return null, or empty objects.
      throw new ReceiveException(e);
    }
  }

  /**
   * Detects and decodes QR codes found within a collection of
   * YUV images. Designed to interface with QRStream Android application.
   *
   * @param qrCodeImages The collection of YUV images to decode
   * @return The data decoded from collection of detected QR codes.
   * @throws ReceiveException If {@code qrCodeImages} failed to receive enough
   * images to complete the data transmission.
   */

  public byte[] decodeQRCodes (IImageProvider frameManager) throws ReceiveException {
    // The received data and track transmission status.
    DecodedMessage message = new DecodedMessage(progress);

    // Try decoding frames only while external application
    // is running and waiting for a response.
    while( frameManager.isRunning() ) {
      // TODO Try improving performance by spawning new thread run each image decoding
      YuvImage img = frameManager.captureFrameFromCamera();
      if (img == null) {
        // Communicate failed state to progress indicator.
        System.out.println("decodeQRCodes: received invalid frame (null)");
        message.setFailedDecoding();
        throw new ReceiveException("Transmission failed to receive a valid frame from the camera");
      }
      try {
        Iterable<Result> res = ImmutableList.of(decodeSingleQRCode(img.getYuvData()));
        displayQRFinderPoints(res);
        State s = saveMessageAndUpdateProgress(res, message);

        if(s == State.Final) {
          System.out.println("decodeQRCodes: Hit final state");
          break;
        }
      } catch (NotFoundException e) {
        // Unable to detect QR in this image, try next one.
        displayQRFinderPoints(NO_RESULTS);
        continue;
      } catch (ReceiveException e) {
        // Encountered invalid QR code during parsing, try next image.
        displayQRFinderPoints(NO_RESULTS);
        continue;
      }
    }
    // Either message complete or received partial message
    // and asked to stop the decoding process.
    if (!message.isComplete()) {
      //Transmission shut down before full message could be read.
      message.setFailedDecoding();
    }
    return message.getEntireMessage();
  }

  /**
   * Detect and decode QR code from an image.
   * @param yuvData The YUV image data containing a single QR code.
   * @return The detected QR code {@code Result} type.
   * @throws ReceiveException If no QR code has been detected or decoding failed.
   */
  protected Result decodeSingleQRCode(byte[] yuvData) throws NotFoundException {
    LuminanceSource src = new PlanarYUVLuminanceSource(yuvData,
        width, height, 0, 0, width, height, false);
    return decodeSingle(src);
  }

  /**
   * Identify the chunk of data decoded from the QR code and
   * add its message to the collection of already received chunks.
   *
   * @param decodedQR The result of decoding QR code(s) from an image
   * @param receivedData The data decoded from prior images.
   * @return The {@code State} indicating whether the whole message has been received.
   * @throws ReceiveException if all of the decoded QR codes have an invalid format
   */
  protected State saveMessageAndUpdateProgress(Iterable<Result> decodedQR, DecodedMessage receivedData)
      throws ReceiveException {
    boolean allInvalid = true;
    String  receiveExceptionMsg = "";
    State state = State.Initial;

    for (Result qr : decodedQR ) {
      try {
        State s = saveMessageAndUpdateProgress(qr, receivedData);
        if (allInvalid) {
          // Found at least one valid QR code. Now it's okay to
          // ignore any other ReceiveException errors since
          // decodeQRCodes would just skip it and try another frame.
          allInvalid = false;
        }
        if (state != s) {
          state = s;
        }
        // No need to continue with other QR codes if message complete
        if (state == State.Final) {
          break;
        }
      }catch(ReceiveException e) {
        // Save one of the errors for later, but try other QR codes
        receiveExceptionMsg = e.getMessage();
      }
    }
    if (allInvalid) {
      throw new ReceiveException(receiveExceptionMsg);
    }
    return state;
  }

  private State saveMessageAndUpdateProgress(Result decodedQR, DecodedMessage receivedData)
      throws ReceiveException {
    PartialMessage messagePart = PartialMessage.createFromResult(decodedQR);
    return receivedData.saveMessageChunk(messagePart);
  }

  /**
   * Update IProgress with QR finder points that were
   * found during the QR decoding. It orders the (x,y)
   * coordinates as follows: [x1,y2,x2,y2...xi,yi...].
   */
  private void displayQRFinderPoints(Iterable<Result> decodedQRCodes) {
    List<Float> list = Lists.newArrayList();
    for (Result qr : decodedQRCodes) {
      ResultPoint[] points = qr.getResultPoints();
      if(points != null) {
        for (ResultPoint point : points) {
          if (point != null) {
            list.add(point.getX());
            list.add(point.getY());
          }
        }
      }
    }
    progress.drawFinderPoints(Floats.toArray(list));
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
   * @throws NotFoundException if there was problem detecting or decoding QR
   * code from image {@source lumSrc}.
   */
  protected static Result decodeSingle(LuminanceSource lumSrc) throws NotFoundException {
    return decodeSingle(lumSrc, Receive.getDecodeHints());
  }

  /**
   * Detects and decode QR code from a luminance image.
   * @param lumSrc The luminance image containing a QR code to decode.
   * @param hints Hints to help the ZXing barcode reader find QR code easier
   * @throws NotFoundException if there was problem detecting or decoding QR
   * code from image {@source lumSrc}.
   */
  protected static Result decodeSingle(LuminanceSource lumSrc,
                                       Map<DecodeHintType,?> hints) throws NotFoundException {
    BinaryBitmap bmap = toBinaryBitmap(lumSrc);
    return new MultiFormatReader().decode(bmap, hints);
  }

  /*
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
