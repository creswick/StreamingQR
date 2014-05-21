package com.galois.qrstream.qrpipe;

import com.galois.qrstream.image.YuvImage;

public interface ICaptureFrame {
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
