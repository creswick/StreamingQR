package com.galois.qrstream.lib;

import android.app.Fragment;

import com.galois.qrstream.qrpipe.Receive;

/**
 * Created by donp on 2/24/14.
 */
public class QrpipeFragment extends Fragment {
    Receive qrpipe;

    @Override
    public void onStart() {
        super.onStart();
        qrpipe = new Receive(640, 480);
    }

}
