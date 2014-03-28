package com.galois.qrstream.lib;

import android.util.Log;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by donp on 2/28/14.
 */
public class DecodeThread extends Thread {
    private final Receive receiver;
    private final BlockingQueue<YuvImage> queue;

    public DecodeThread(Receive receiver, BlockingQueue<YuvImage> queue) {
        this.receiver = receiver;
        this.queue = queue;
    }

    @Override
    public void run(){
        byte[] message;
        try {
            message = receiver.decodeQRCodes(queue);
            Log.d(Constants.APP_TAG, "DecodeThread heard " + message);
        } catch(ReceiveException e) {
            e.printStackTrace();
        }
    }
}
