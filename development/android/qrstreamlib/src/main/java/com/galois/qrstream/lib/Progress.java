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
    public final static int INITIAL = 1;
    public final static int INTERMEDIATE = 2;
    public final static int FINAL = 3;
    Handler handler;

    public void setStateHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void changeState(DecodeState state) {
        Bundle changeMsg = new Bundle();
        changeMsg.putString("message", state.toString());
        changeMsg.putSerializable("state", state.getState());

        Message stateChange = Message.obtain();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
