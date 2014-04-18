package com.galois.qrstream.lib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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
        int total_frame_count = state.getData().length();
        if(total_frame_count > 0) {
            int percent_complete = state.getState().ordinal() / total_frame_count;
            changeMsg.putSerializable("percent_complete", percent_complete);
        }
        Message stateChange = Message.obtain();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
