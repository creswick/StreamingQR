/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.lib;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Created by donp on 2/28/14.
 */
public class DecodeThread extends Thread {
    private final Receive receiver;
    private final CameraManager cameraManager;
    private final Context context;

    public DecodeThread(Context ctx, IProgress progress, CameraManager cameraManager) {
        this.context = ctx;
        this.cameraManager = cameraManager;
        this.receiver = new Receive(
                cameraManager.getDisplayHeight(),
                cameraManager.getDisplayWidth(),
                progress);
    }

    @Override
    public void run(){
        Job message;
        try {
            message = (Job)receiver.decodeQRSerializable(cameraManager);
            Log.w(Constants.APP_TAG, "DecodeThread read message of length: " + message.getData().length);
            Log.w(Constants.APP_TAG, "DecodeThread heard " + message.toString());


            Intent i = new Intent();
            i.setAction(Intent.ACTION_SEND);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setType(message.getMimeType());

            // this should conditionally use a URI if the payload is too large.
            URI dataLoc = storeData(message);
            i.putExtra(Intent.EXTRA_STREAM, dataLoc);

            // TODO integrate with ZXing

            context.startActivity(Intent.createChooser(i, "Open with"));
        } catch(ReceiveException e) {
            Log.e(Constants.APP_TAG, "DecodeThread failed to read message. " + e.getMessage());
        } catch (IOException e) {
            Log.e(Constants.APP_TAG, "Could not store data to temp file." + e.getMessage());
        }
    }

    private @NotNull URI storeData(Job message) throws IOException {
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
