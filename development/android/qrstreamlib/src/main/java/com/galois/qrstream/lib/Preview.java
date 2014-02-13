package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("qrstream", "previewFrame data "+data.length);
    }
}
