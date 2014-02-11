package com.galois.qrstream.lib;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by donp on 2/11/14.
 */
public class CameraFragment extends Fragment {

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.camera_fragment, container, false);
        return rootView;
    }
}
