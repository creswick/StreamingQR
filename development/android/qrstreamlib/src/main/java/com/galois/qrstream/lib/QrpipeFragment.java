package com.galois.qrstream.lib;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Handler;
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
        frameQueue = new ArrayBlockingQueue<YuvImage>(1);
    }

    public void startPipe(Camera.Parameters params, Handler handler) {
        if(decodeThread == null) {
            Camera.Size previewSize = params.getPreviewSize();
            Progress progress = new Progress();
            progress.setStateHandler(handler);
            receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width, progress);
            decodeThread = new DecodeThread();
            decodeThread.setReceiver(receiveQrpipe);
            decodeThread.setQueue(frameQueue);
            decodeThread.start();
        } else {
            Log.e(APP_TAG, "Error: DecodeThread already running");
        }
    }

    public void stopPipe() {
        // Threads can only be suggested to stop
        decodeThread.end();
    }
}
