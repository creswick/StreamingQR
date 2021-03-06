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
package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.IImageProvider;
import com.google.common.collect.Queues;

import org.jetbrains.annotations.NotNull;

/**
 * CameraManager services requests for a preview frame from the camera
 */
public final class CameraManager implements IImageProvider, Camera.PreviewCallback {

    // When isRunning is false it signals that the camera is not available
    // and any decoding of QR in progress should be stopped.
    private boolean isRunning = false;

    private final BlockingQueue<YuvImage> currentFrame = Queues.newSynchronousQueue();

    private final Camera camera;
    private final int displayWidth;
    private final int displayHeight;

    public CameraManager(@NotNull Camera camera) {
        // The preview mode is always in 'Landscape' mode (ex. h=720, w=1280)
        Camera.Size previewSize = camera.getParameters().getPreviewSize();

        // For now we assume that the camera is initialized elsewhere, is open, and preview is running
        this.camera = camera;
        this.displayWidth = previewSize.width;
        this.displayHeight = previewSize.height;
        this.isRunning = true;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }
    public int getDisplayHeight() {
        return displayHeight;
    }

    @Override
    public void onPreviewFrame(@NotNull byte[] data, @NotNull Camera camera) {

        // Camera.PreviewCallback requests get handled on the same thread that opened the camera.
        // It so happens that the camera was opened on a separate thread from the main UI thread.
        if (currentFrame.offer(new YuvImage(data, displayWidth, displayHeight))) {
            Log.d(Constants.APP_TAG, "CameraManager set currentFrame.");
        } else {
            Log.e(Constants.APP_TAG, "CameraManager tried to set currentFrame before successful read.");
        }
    }

    public synchronized void stopRunning() {
        // Release camera and callback
        isRunning = false;
        camera.setPreviewCallback(null);
    }

    // When isRunning is false, it signals that camera and preview callback are not valid
    // and that any decoding of QR codes should stop.
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    // Requests for preview frame originate from the QRlib Rx.decodeQRCodes method
    // We want that thread to fire off a preview callback request from camera
    // and then block until the result gets inserted into the currentFrame.
    @Override
    public synchronized YuvImage captureFrameFromCamera() {
        // Only one thread at a time can request a frame from the camera
        setupOneShotPreviewCallback();
        return getFrame();
    }

    /**
     * If the camera is running, getFrame will block and waits for
     * the Camera.PreviewCallback to give it a frame. It will return
     * {@code null} if the PreviewCallback does not get a frame within
     * {@code Constants.RECEIVE_TIMEOUT_MS} milliseconds, or camera
     * has been asked to stop running.
     */
    private synchronized YuvImage getFrame() {
        YuvImage img = null;
        if (isRunning) {
            try {
                img = currentFrame.poll(Constants.RECEIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Interrupted while waiting for image. Log interruption and return null frame.
                Log.v(Constants.APP_TAG, "CameraManager interrupted while waiting for image.");
            }
        }
        return img;
    }

    // Setup camera callback to handle next preview frame
    private synchronized void setupOneShotPreviewCallback() {
        if (isRunning) {
            camera.setOneShotPreviewCallback(this);
        }else{
            Log.e(Constants.APP_TAG, "setupPreviewCallback called but " +
                    "CameraManager NOT running, Thread #" + Thread.currentThread().getId());
        }
    }
}
