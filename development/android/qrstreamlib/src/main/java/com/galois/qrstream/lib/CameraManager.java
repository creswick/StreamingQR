package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.ICaptureFrame;
import com.google.common.collect.Queues;

import org.jetbrains.annotations.NotNull;

/**
 * CameraManager services requests for a preview frame from the camera
 */
public class CameraManager implements ICaptureFrame {

    private Camera camera;
    private Preview previewCallback;

    // When isRunning is false it signals that the camera is not available
    // and any decoding of QR in progress should be stopped.
    private boolean isRunning = false;

    private final BlockingQueue<YuvImage> currentFrame = Queues.newSynchronousQueue();

    // Handler is bound to the same thread that created the CameraManager
    // i.e. the UI thread.  Perhaps this should get moved?
    private final Handler frameHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj != null) {
                if(currentFrame.offer((YuvImage) msg.obj) == false) {
                    Log.e(Constants.APP_TAG, "CameraManager tried to set currentFrame before successful read.");
                }else {
                    Log.d(Constants.APP_TAG, "CameraManager set currentFrame.");
                }
            }else{
                // Probably not a big deal as it would just cause qrlib to stop decoding QR codes
                Log.d(Constants.APP_TAG, "CameraManager asked to handle NULL message.");
            }
        }
    };

    public void startRunning(@NotNull Camera camera,
                             @NotNull Preview previewCallback) {
        this.camera = camera;
        this.previewCallback = previewCallback;
        this.isRunning = true;

        camera.setPreviewCallback(null);
    }

    public void stopRunning() {
        Log.e(Constants.APP_TAG, "CameraManager stopRunning called.");
        // Release camera and callback
        if (camera != null && previewCallback != null) {
            isRunning = false;
            camera.setPreviewCallback(null);
            camera = null;
            previewCallback = null;
        }
    }

    // When isRunning is false, it signals that camera and preview callback are not valid
    // and that any decoding of QR codes should stop.
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public synchronized YuvImage captureFrameFromCamera() {
        // Only one thread at a time can request a frame from the camera
        setupOneShotPreviewCallback();
        return getFrame();
    }

    // Waits for preview frame from frameHandler before returning.
    private YuvImage getFrame() {
        YuvImage img = null;
        try {
            img = currentFrame.poll(Constants.RECEIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Interrupted while waiting for image. Log interruption and return null frame.
            Log.v(Constants.APP_TAG, "CameraManager interrupted while waiting for image.");
        }
        if(img == null) {
            Log.v(Constants.APP_TAG, "CameraManager received null preview frame.");
        }
        return img;
    }

    // Setup camera callback to handle next preview frame
    private void setupOneShotPreviewCallback() {
        if ((camera != null) && (previewCallback != null)) {
            previewCallback.setHandler(frameHandler);
            camera.setOneShotPreviewCallback(previewCallback);
        } else {
            if (camera == null) {
                Log.e(Constants.APP_TAG, "Cannot request preview frame when camera is not initialized.");
            }
            if (previewCallback == null) {
                Log.e(Constants.APP_TAG, "Cannot request preview frame from camera without " +
                        "first specifying a handler for the preview frames.");
            }
        }
    }
}
