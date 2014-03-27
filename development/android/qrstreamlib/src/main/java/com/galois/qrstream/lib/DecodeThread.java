package com.galois.qrstream.lib;

import android.util.Log;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by donp on 2/28/14.
 */
public class DecodeThread extends Thread {
    private Receive receiver;
    public volatile boolean cont = true;
    private ArrayBlockingQueue<YuvImage> queue;

    public void setReceiver(Receive receiver) { this.receiver = receiver; }
    public void setQueue(ArrayBlockingQueue<YuvImage> queue) { this.queue = queue; }
    public void end(){ this.cont = false; }

    @Override
    public void run(){
        byte[] message;
        try {
            message = receiver.decodeQRCodes(queue);
            Log.d(Constants.APP_TAG, "DecodeThread heard " + message);
        } catch(ReceiveException e) {

        }
    }
}
