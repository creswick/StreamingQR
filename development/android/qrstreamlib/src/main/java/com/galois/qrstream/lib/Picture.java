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
import android.util.Log;

/**
 * Created by donp on 2/12/14.
 */
public class Picture implements Camera.PictureCallback {
    private Handler ui;
    private String label;

    public Picture(Handler ui, String label) {
        this.ui = ui;
        this.label = label;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        String dataReport = "";
        dataReport = ""+data.length+" bytes";
        Log.d("qrstream", "Picture taken - "+dataReport+" callback "+label);
    }
}
