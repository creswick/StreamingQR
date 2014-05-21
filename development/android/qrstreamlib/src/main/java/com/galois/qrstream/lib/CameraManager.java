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
    private boolean isRunning = false;

    private final BlockingQueue<YuvImage> currentFrame = Queues.newSynchronousQueue();

    // Handler is bound to the same thread that created the CameraManager
    // i.e. the UI thread.  Perhaps this should get moved?
    private final Handler frameHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (isRunning && msg.obj != null) {
                Log.d(Constants.APP_TAG, "CameraManager about to setFrame.");
                if(currentFrame.offer((YuvImage) msg.obj) == false) {
                    Log.e(Constants.APP_TAG, "CameraManager trying to setFrame before successful read.");
                }else {
                    Log.d(Constants.APP_TAG, "CameraManager finished setFrame.");
                }
            }else{
                if(isRunning && msg.obj == null) {
                    Log.e(Constants.APP_TAG, "CameraManager received empty message.");
                }else {
                    Log.d(Constants.APP_TAG, "CameraManager not running, ignoring handleMessage request.");
                }
            }
        }
    };

    public void startRunning(@NotNull Camera camera,
                             @NotNull Preview previewCallback) {
        this.camera = camera;
        this.previewCallback = previewCallback;
        this.isRunning = true;

        previewCallback.setHandler(frameHandler);
        camera.setPreviewCallback(null);
    }

    public void stopRunning() {
        Log.e(Constants.APP_TAG, "CameraManager stopRunning called.");
        isRunning = false;
        // Release camera and callback
        if (camera != null && previewCallback != null) {
            // TODO are there any unhandled messages that we need to remove?
            Log.e(Constants.APP_TAG, "CameraManager stopRunning: camera ref is null.");
            camera = null;
            previewCallback = null;
        }
    }

    // When isRunning is false, it signals that camera and preview callback are not valid
    // and that any decoding of QR codes should stop.
    public boolean isRunning() {
        return isRunning;
    }


    public YuvImage captureFrameFromCamera() {
        if (! isRunning ) {
            return null;
        }

        if (camera == null) {
            Log.e(Constants.APP_TAG, "XXX: Why is camera null!? GRRR!!");
        }
        if (previewCallback == null) {
            Log.e(Constants.APP_TAG, "XXX: Why is previewCallback null!? GRRR!!");
        }

        previewCallback.setHandler(frameHandler);
        camera.setOneShotPreviewCallback(previewCallback);

        // We need to wait for framehandler to set img before it can be returned
        YuvImage img = null;
        try {
            img = currentFrame.poll(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Interrupted while waiting for image
            // Log interruption and decide what to do
            // ? add: while ( img == null ), poll again... wait longer
        }
        if(img == null) {
            Log.e(Constants.APP_TAG, "captureFrameFromCamera got null frame.");
        }
        return img;

    }

    // Setup camera callback to handle next preview frame
    private void setupOneShotPreviewCallback() {
        if ((camera != null) && (previewCallback != null)) {
            camera.setOneShotPreviewCallback(previewCallback);
        } else {
            if (camera == null) {
                Log.e(Constants.APP_TAG, "XXX: Why is camera null!? GRRR!!");
            }
            if (previewCallback == null) {
                Log.e(Constants.APP_TAG, "XXX: Why is previewCallback null!? GRRR!!");
            }
        }
    }
}
