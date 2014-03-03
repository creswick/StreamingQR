package com.galois.qrstream.lib;

import android.app.Fragment;
import android.hardware.Camera;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by donp on 2/24/14.
 */
public class QrpipeFragment extends Fragment implements Constants {
    Receive receiveQrpipe;
    ArrayBlockingQueue frameQueue;
    DecodeThread decodeThread;

    @Override
    public void onStart() {
        super.onStart();
    }

    public void startPipe(Camera.Parameters params) {
        if(decodeThread == null) {
            Camera.Size previewSize = params.getPreviewSize();
            receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width);
            frameQueue = new ArrayBlockingQueue<YuvImage>(1);
            decodeThread = new DecodeThread();
            decodeThread.setReceiver(receiveQrpipe);
            decodeThread.start();
        } else {
            Log.e(APP_TAG, "Error: DecodeThread already running");
        }
    }

    public void stopPipe() {
        // Threads can only be suggested to stop
        decodeThread.cont = false;
    }
}
