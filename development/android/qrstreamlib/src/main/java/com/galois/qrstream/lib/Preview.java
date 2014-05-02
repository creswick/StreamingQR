package com.galois.qrstream.lib;


import android.hardware.Camera;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    private Queue frames;
    private int height;
    private int width;

    public Preview(@NotNull Queue frames, @NotNull Camera.Size size) {
        setQueue(frames);
        setSize(size);
    }

    public void setQueue(@NotNull Queue frames) {
        this.frames = frames;
    }

    public void setSize(@NotNull Camera.Size size) {
        this.height = size.height;
        this.width = size.width;
    }

    @Override
    public void onPreviewFrame(@NotNull byte[] data, @NotNull Camera camera) {
        Log.v(Constants.APP_TAG, "onPreviewFrame data len "+data.length+" queue len "+frames.size());
        YuvImage frame = new YuvImage(data, height, width);
        String fullFlag = "";
        if(frames.offer(frame) == false) {
            fullFlag = "(full)";
        }
        Log.v(Constants.APP_TAG, "onPreviewFrame: data len="+data.length
              +" queue len="+frames.size() + " "+fullFlag);
    }
}
