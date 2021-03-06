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

import com.galois.qrstream.image.YuvImage;

public interface IImageProvider {
  /**
   * Return the YUV image data from an android camera.
   *
   * @return The yuv image from the Android device
   */
  public YuvImage captureFrameFromCamera();

  /**
   * Return true if application is running and false if decoding should stop.
   */
  public boolean isRunning();
}
