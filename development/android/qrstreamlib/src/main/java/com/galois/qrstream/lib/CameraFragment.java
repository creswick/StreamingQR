package com.galois.qrstream.lib;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

/**
 * Created by donp on 2/11/14.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView camera_window;

    public CameraFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.camera_fragment, container, false);

        camera_window = (SurfaceView)rootView.findViewById(R.id.camera_window);
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        camera = Camera.open();
        SurfaceHolder holder = camera_window.getHolder();
        holder.addCallback(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
