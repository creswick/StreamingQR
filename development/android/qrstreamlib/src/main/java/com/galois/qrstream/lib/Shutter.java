package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by donp on 2/13/14.
 */
public class Shutter implements Camera.ShutterCallback {
    @Override
    public void onShutter() {
        Log.d("qrstream", "onShutter");
    }
}
