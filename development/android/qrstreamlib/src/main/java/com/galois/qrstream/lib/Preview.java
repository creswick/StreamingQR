package com.galois.qrstream.lib;


import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    private int height;
    private int width;
    private Handler previewHandler;

    public Preview(@NotNull Camera.Size size) {
        setSize(size);
    }

    public void setHandler(@NotNull Handler previewHandler) {
        this.previewHandler = previewHandler;
    }

    public void setSize(@NotNull Camera.Size size) {
        this.height = size.height;
        this.width = size.width;
    }

    @Override
    public void onPreviewFrame(@NotNull byte[] data, @NotNull Camera camera) {
        Log.v(Constants.APP_TAG, "onPreviewFrame data len "+data.length);

        Handler thePreviewHandler = previewHandler;
        if (thePreviewHandler != null) {
            Message message = thePreviewHandler.obtainMessage(R.id.decode, new YuvImage(data, width, height));
            message.sendToTarget();
            previewHandler = null;
        }else{
            Log.e(Constants.APP_TAG, "onPreviewFrame called with no previewHandler set. It is null");
        }
    }
}
