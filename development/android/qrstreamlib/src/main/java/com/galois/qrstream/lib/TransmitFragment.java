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

import java.io.IOException;

/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends Fragment implements SurfaceHolder.Callback {

    static Camera camera;
    SurfaceView camera_window;
    Button capture;
    static Handler ui;

    public TransmitFragment() {
        ui = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transmit_fragment, container, false);

        camera_window = (SurfaceView)rootView.findViewById(R.id.flash_window);
        capture = (Button)rootView.findViewWithTag("send");
        capture.setOnClickListener(new CaptureClick());
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        // 640x480 = 3110400 byte frame
        camera.setPreviewCallback(new Preview());
        camera_window.getHolder().addCallback(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        camera.setPreviewCallback(null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }

    public static class CaptureClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Log.d("qstream", "Send Pushed");
        }
    }
}
