package com.galois.qrstream.qrpipe;

import java.util.Iterator;

import com.galois.qrstream.image.YuvImage;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of IImageProvider that takes an {@code Iterable} of images
 * and presents them as requested by the Receiver in {@link Receive#decodeQRCodes}
 */
public class FrameProvider implements IImageProvider {

  public static final FrameProvider INVALID_COLLECTION = new InvalidFrameProvider();

  private final Iterable<YuvImage> yuvFrames;
  private final Iterator<YuvImage> yuvIter;

  public FrameProvider (Iterable<YuvImage> frames) {
    yuvFrames = frames;
    yuvIter = yuvFrames.iterator();
  }

  /*
   * Serves up a single image when requested.
   */
  public FrameProvider (YuvImage frame) {
    yuvFrames = ImmutableList.of(frame);
    yuvIter = yuvFrames.iterator();
  }

  @Override
  public YuvImage captureFrameFromCamera() {
    if(yuvIter.hasNext()) {
      return yuvIter.next();
    }
    return null;
  }

  @Override
  public boolean isRunning() {
    return (yuvIter != null && yuvIter.hasNext());
  }
}

/**
 * Implementation of IImageProvider that will cause decodeQRCodes to fail.
 */
class InvalidFrameProvider extends FrameProvider {

  private static final Iterable<YuvImage> empty = ImmutableList.of();

  public InvalidFrameProvider() {
    super(empty);
  }

  // Always return true to check exception thrown when
  // decode routine wants more frames but nothing valid returned.
  @Override
  public boolean isRunning() {
    return true;
  }
}
