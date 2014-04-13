package com.galois.qrstream.lib;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;
import com.google.common.base.Charsets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by donp on 2/28/14.
 */
public class DecodeThread extends Thread {
    private final Receive receiver;
    private final BlockingQueue<YuvImage> queue;
    private final Context context;

    public DecodeThread(Context ctx, Receive receiver, BlockingQueue<YuvImage> queue) {
        this.context = ctx;
        this.receiver = receiver;
        this.queue = queue;
    }

    @Override
    public void run(){
        Job message;
        try {
            message = (Job)receiver.decodeQRSerializable(queue);
            Log.w(Constants.APP_TAG, "DecodeThread read message of length: " + message.getData().length);
            // We'll  need to read MIME type later, but for now, we
            // assume we have text input.
            //String msg = new String(message, Charsets.ISO_8859_1);
            Log.w(Constants.APP_TAG, "DecodeThread heard " + message.toString());

            URI dataLoc = storeData(message);

            Intent i = new Intent();
            i.setAction(Intent.ACTION_SEND);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setType(message.getMimeType());
            i.putExtra(Intent.EXTRA_STREAM, dataLoc);
            context.startActivity(Intent.createChooser(i, "Open with"));
        } catch(ReceiveException e) {
            Log.e(Constants.APP_TAG, "DecodeThread failed to read message. " + e.getMessage());
        } catch (IOException e) {
            Log.e(Constants.APP_TAG, "Could not store data to temp file." + e.getMessage());
        }
    }

    private URI storeData(Job message) throws IOException {
        File cacheDir = context.getCacheDir();
        File tmpFile = File.createTempFile("qrstream","", cacheDir);

        // make tmpFile world-readable:
        tmpFile.setReadable(true, false);
        tmpFile.deleteOnExit();

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
            bos.write(message.getData());
            bos.flush();
        } finally {
            if ( null != bos) {
                bos.close();
            }
        }
        return tmpFile.toURI();
    }
}
