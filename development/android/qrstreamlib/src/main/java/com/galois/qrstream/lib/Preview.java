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


import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.galois.qrstream.image.YuvImage;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;

/**
 * Created by donp on 2/13/14.
 */
public class Preview implements Camera.PreviewCallback {
    private int height;
    private int width;
    private Handler previewHandler;

    public Preview(@NotNull Camera.Size size) {
        setSize(size);
    }

    public void setHandler(@NotNull Handler previewHandler) {
        this.previewHandler = previewHandler;
    }

    public void setSize(@NotNull Camera.Size size) {
        this.height = size.height;
        this.width = size.width;
    }

    @Override
    public void onPreviewFrame(@NotNull byte[] data, @NotNull Camera camera) {
        Log.v(Constants.APP_TAG, "onPreviewFrame data len "+data.length);

        Handler thePreviewHandler = previewHandler;
        if (thePreviewHandler != null) {
            Message message = thePreviewHandler.obtainMessage(R.id.decode, new YuvImage(data, width, height));
            message.sendToTarget();
            previewHandler = null;
        }else{
            Log.e(Constants.APP_TAG, "onPreviewFrame called with no previewHandler set. It is null");
        }
    }
}
