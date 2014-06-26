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

/**
 * Created by donp on 3/3/14.
 */
public final class Constants {
    public static final String APP_TAG = "qrstream";
    public static final int RECEIVE_TIMEOUT_MS = 10000;
    public static final String MIME_TYPE_TEXT_NOTE = "application/x-vnd.galois.qrstream.text-note";

    /**
     * The maximum number of QR codes that we expect to need during a reasonable transmission.
     *
     * Our fastest transmission is currently showing a new frame every 200ms, or 5 fps.  If we assume
     * a  maximum transmission time of 10 min, we end up with the following number of chunks:
     */
    public static final int MAX_CHUNKS = (1000 / 200) * 60 * 10;

    // Do not allow class to be instantiated.
    // Reference constants by Constants.APP_TAG
    private Constants() {
        throw new AssertionError("Unexpected instantiation of private Constants class.");
    }
}
