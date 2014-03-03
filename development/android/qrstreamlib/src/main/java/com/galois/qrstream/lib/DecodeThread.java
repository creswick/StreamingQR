package com.galois.qrstream.lib;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;

import java.util.Queue;

/**
 * Created by donp on 2/28/14.
 */
public class DecodeThread extends Thread {
    private Receive receiver;
    public volatile boolean cont = true;
    private Queue<YuvImage> queue;

    public void setReceiver(Receive receiver) { this.receiver = receiver; }
    public void setQueue(Queue<YuvImage> queue) { this.queue = queue; }
    public void end(){ this.cont = false; }

    @Override
    public void run(){
        while(cont) {
            receiver.decodeQRCodes(queue);
        }
    }
}
