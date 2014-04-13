package com.galois.qrstream.lib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.galois.qrstream.qrpipe.DecodeState;
import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.State;

import org.jetbrains.annotations.NotNull;

/**
 * Created by donp on 3/5/14.
 */
public class Progress implements IProgress {
    private Handler handler;

    public void setStateHandler(@NotNull Handler handler){
        this.handler = handler;
    }

    @Override
    public void changeState(@NotNull DecodeState state) {
        Bundle changeMsg = new Bundle();
        changeMsg.putString("message", state.toString());
        changeMsg.putSerializable("state", state.getState());

        Message stateChange = Message.obtain();
        stateChange.setData(changeMsg);
        handler.dispatchMessage(stateChange);
    }
}
