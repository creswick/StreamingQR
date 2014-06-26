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
