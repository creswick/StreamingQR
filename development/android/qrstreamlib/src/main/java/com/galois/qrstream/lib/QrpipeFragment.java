package com.galois.qrstream.lib;

import android.app.Fragment;

import com.galois.qrstream.qrpipe.Manager;

/**
 * Created by donp on 2/24/14.
 */
public class QrpipeFragment extends Fragment {
    Manager qrpipe;

    @Override
    public void onStart() {
        super.onStart();
        qrpipe = new Manager();
    }

}
