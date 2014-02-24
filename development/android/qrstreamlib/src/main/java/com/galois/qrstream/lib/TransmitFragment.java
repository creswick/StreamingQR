package com.galois.qrstream.lib;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends QrpipeFragment {

    static Camera camera;
    ImageView send_window;
    Button capture;
    static Handler ui;

    public TransmitFragment() {
        ui = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transmit_fragment, container, false);

        send_window = (ImageView)rootView.findViewById(R.id.send_window);
        capture = (Button)rootView.findViewWithTag("send");
        capture.setOnClickListener(new CaptureClick());
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        send_window.setImageDrawable(getResources().getDrawable(R.drawable.sample1));
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    public static class CaptureClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Log.d("qstream", "Send Pushed");
        }
    }
}
