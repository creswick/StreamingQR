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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.galois.qrstream.qrpipe.DecodeState;
import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.State;

import org.jetbrains.annotations.NotNull;

/**
 * Created by donp on 3/5/14.
 */
public class Progress implements IProgress {
    private final Handler handler;

    public Progress(@NotNull Handler handler){
        this.handler = handler;
    }

    @Override
    public void changeState(@NotNull DecodeState state) {
        Bundle changeMsg = new Bundle();
        changeMsg.putString("message", state.toString());
        changeMsg.putSerializable("state", state.getState());

        Log.d(Constants.APP_TAG, "changeState state = " + state.getState());
        if(state.getState() == State.Intermediate) {
            int total_frame_count = state.getCapacity();
            int num_frames_decoded = state.getTotalFramesDecoded();
            int percent_complete = (int)((num_frames_decoded / (float)total_frame_count)*100);
            changeMsg.putSerializable("chunk_count", num_frames_decoded);
            changeMsg.putSerializable("chunk_id", state.getMostRecentChunkId());
            changeMsg.putSerializable("chunk_total", total_frame_count);
            changeMsg.putSerializable("percent_complete", percent_complete);
            Log.d(Constants.APP_TAG, "changeState handler, " + num_frames_decoded +
                                        "/" + total_frame_count +
                                        " " +percent_complete + "%");
        }

        Message stateChange = Message.obtain();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
