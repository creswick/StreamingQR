package com.galois.qrstream.lib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.galois.qrstream.qrpipe.DecodeState;
import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.State;

/**
 * Created by donp on 3/5/14.
 */
public class Progress implements IProgress {
    private Handler handler;

    public void setStateHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void changeState(DecodeState state) {
        Bundle changeMsg = new Bundle();
        changeMsg.putString("message", state.toString());
        changeMsg.putSerializable("state", state.getState());

        Log.d(Constants.APP_TAG, "changeState state = " + state.getState());
        if(state.getState() == State.Intermediate) {
            int total_frame_count = state.getCapacity();
            int num_frames_decoded = state.getTotalFramesDecoded();
            int percent_complete = (int)((num_frames_decoded / (float)total_frame_count)*100);
            changeMsg.putSerializable("percent_complete", percent_complete);
            Log.d(Constants.APP_TAG, "changeState handler, total " + total_frame_count + " received " + num_frames_decoded + " percent "+percent_complete);
        }

        Message stateChange = Message.obtain();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
