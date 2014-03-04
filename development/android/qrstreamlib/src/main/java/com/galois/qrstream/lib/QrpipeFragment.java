package com.galois.qrstream.lib;

import android.app.Fragment;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by donp on 2/24/14.
 */
public class QrpipeFragment extends Fragment {
    Receive receiveQrpipe;
    ArrayBlockingQueue frameQueue;

    @Override
    public void onStart() {
        super.onStart();
        receiveQrpipe = new Receive(640, 480);
        frameQueue = new ArrayBlockingQueue<YuvImage>(1);
    }

}
