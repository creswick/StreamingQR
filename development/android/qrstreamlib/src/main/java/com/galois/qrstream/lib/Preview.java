package com.galois.qrstream.lib;


import android.hardware.Camera;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;

import java.util.Queue;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    Queue frames;
    int height;
    int width;

    public void setQueue(Queue frames) {
        this.frames = frames;
    }
    public void setHeight(int h) { this.height = h; }
    public void setWidth(int w) { this.width = w; }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("qrstream", "previewFrame data "+data.length);
        YuvImage frame = new YuvImage(data, height, width);
        frames.add(frame);
    }
}
