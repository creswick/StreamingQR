package com.galois.qrstream.lib;

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
public class ReceiveFragment extends QrpipeFragment implements SurfaceHolder.Callback {

    static Camera camera;
    SurfaceView camera_window;
    Button capture;
    static Handler ui;

    public ReceiveFragment() {
        ui = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.receive_fragment, container, false);

        camera_window = (SurfaceView)rootView.findViewById(R.id.camera_window);
        capture = (Button)rootView.findViewWithTag("capture");
        capture.setOnClickListener(new CaptureClick());
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        Preview previewCallback = new Preview();
        previewSetup(params, previewCallback);
        camera.setPreviewCallback(previewCallback);
        camera_window.getHolder().addCallback(this);
    }

    private void previewSetup(Camera.Parameters params, Preview previewCallback) {
        previewCallback.setQueue(frameQueue);
        previewCallback.setHeight(params.getPreviewSize().height);
        previewCallback.setWidth(params.getPreviewSize().width);
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
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }

    public static class TakePicture implements Runnable {
        public void run() {
            Log.d("qrstream", "** Taking picture");
            camera.takePicture(new Shutter(), new Picture(ui, "raw"),
                                              new Picture(ui, "postdata"),
                                              new Picture(ui, "jpeg"));
        }
    }

    public static class StartPreview implements Runnable {
        public void run() {
            Log.d("qrstream", "** Start preview");
            camera.startPreview();
        }
    }

    public static class CaptureClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Log.d("qstream", "Capture Pushed");
            Thread thread = new Thread(new TakePicture());
            thread.run();
        }
    }
}
