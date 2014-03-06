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
    Handler handler;

    public void setStateHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void changeState(DecodeState state) {
        Bundle changeMsg = new Bundle();
        switch(state.getState()) {
            case Initial:
                changeMsg.putString("message", "Initial state");
                break;
            case Intermediate:
                changeMsg.putString("message", "Intermediate state");
                break;
            case Final:
                changeMsg.putString("message", "Final state");
                break;
        }
        Message stateChange = new Message();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
