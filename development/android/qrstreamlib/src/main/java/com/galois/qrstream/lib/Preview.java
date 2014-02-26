package com.galois.qrstream.lib;

import android.hardware.Camera;
import android.util.Log;

import com.galois.qrstream.qrpipe.Receive;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    Receive qrpipe;

    public void setQrpipe(Receive qrpipe) {
        this.qrpipe = qrpipe;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("qrstream", "previewFrame data "+data.length);
        // qrpipe.decodeQRCodes(null); #Function not yet implemented
    }
}
