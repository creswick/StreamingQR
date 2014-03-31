package com.galois.qrstream.lib;

import android.util.Log;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;
import com.google.common.base.Charsets;

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
            Log.w(Constants.APP_TAG, "DecodeThread read message of length: " + message.length);
            // We'll  need to read MIME type later, but for now, we
            // assume we have text input.
            String msg = new String(message, Charsets.ISO_8859_1);
            Log.w(Constants.APP_TAG, "DecodeThread heard " + msg);
        } catch(ReceiveException e) {
            Log.e(Constants.APP_TAG, "DecodeThread failed to read message. " + e.getMessage());
        }
    }
}
