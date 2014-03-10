package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

/**
 * Created by donp on 2/12/14.
 */
public class Picture implements Camera.PictureCallback {
    private Handler ui;
    private String label;

    public Picture(Handler ui, String label) {
        this.ui = ui;
        this.label = label;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        String dataReport = "";
        dataReport = ""+data.length+" bytes";
        Log.d("qrstream", "Picture taken - "+dataReport+" callback "+label);
    }
}
