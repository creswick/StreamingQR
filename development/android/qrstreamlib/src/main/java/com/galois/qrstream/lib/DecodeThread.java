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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.ReceiveException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

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
                Constants.MAX_CHUNKS,
                progress);
    }

    @Override
    public void run(){
        Job message;
        try {
            message = (Job)receiver.decodeQRSerializable(cameraManager);
            Log.w(Constants.APP_TAG, "DecodeThread received " + message.getData().length + " bytes, " +
                                     "mimetype: " + message.getMimeType());


            Intent i = buildIntent(message);
            context.startActivity(Intent.createChooser(i, "Open with"));
        } catch(ReceiveException e) {
            Log.e(Constants.APP_TAG, "DecodeThread failed to read message. " + e.getMessage());
        } catch (IOException e) {
            Log.e(Constants.APP_TAG, "Could not store data to temp file." + e.getMessage());
        }
    }

    private Intent buildIntent(Job message) throws IOException {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        String mimeType = message.getMimeType();
        i.setType(mimeType);

        if(mimeType.equals("text/plain")) {
            String msg = new String(message.getData());
            i.putExtra(Intent.EXTRA_TEXT, msg);
        } else if (mimeType.equals(Constants.MIME_TYPE_TEXT_NOTE)) {
            String json = new String(message.getData());
            try {
                JSONObject note = new JSONObject(json);
                i.putExtra(Intent.EXTRA_TEXT, note.getString(Intent.EXTRA_TEXT));
                i.putExtra(Intent.EXTRA_SUBJECT, note.getString(Intent.EXTRA_SUBJECT));
                i.setType("text/plain");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // this should conditionally use a URI if the payload is too large.
            URI dataLoc = storeData(message);
            i.putExtra(Intent.EXTRA_STREAM, dataLoc);
        }
        return i;
    }

    private @NotNull URI storeData(Job message) throws IOException {
        File cacheDir = context.getCacheDir();
        File tmpFile = File.createTempFile(Constants.APP_TAG, "", cacheDir);

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
