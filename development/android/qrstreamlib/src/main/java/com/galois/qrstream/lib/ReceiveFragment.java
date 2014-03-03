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

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.Receive;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by donp on 2/11/14.
 */
public class ReceiveFragment extends Fragment implements SurfaceHolder.Callback, Constants {

    private static Camera camera;
    private SurfaceView camera_window;
    private Button capture;
    private static Handler ui;
    private ArrayBlockingQueue frameQueue;
    private Receive receiveQrpipe;
    private DecodeThread decodeThread;


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
<<<<<<< HEAD
        Preview previewCallback = new Preview(frameQueue, params.getPreviewSize());
        camera.setPreviewCallback(previewCallback);
        camera_window.getHolder().addCallback(this);
        startPipe(params);
=======
        previewSetup(camera, params);
        camera_window.getHolder().addCallback(this);
        startPipe(params);
    }

    private void previewSetup(Camera camera, Camera.Parameters params) {
        Preview previewCallback = new Preview();
        previewCallback.setQueue(frameQueue);
        previewCallback.setHeight(params.getPreviewSize().height);
        previewCallback.setWidth(params.getPreviewSize().width);
        camera.setPreviewCallback(previewCallback);
>>>>>>> DecodeThread approach
    }

    @Override
    public void onPause(){
        super.onPause();
        camera.setPreviewCallback(null);
        stopPipe();
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
        }
    }
    public void startPipe(Camera.Parameters params) {
        if(decodeThread == null) {
            Camera.Size previewSize = params.getPreviewSize();
            receiveQrpipe = new Receive(previewSize.height,
                    previewSize.width);
            frameQueue = new ArrayBlockingQueue<YuvImage>(1);
            decodeThread = new DecodeThread();
            decodeThread.setReceiver(receiveQrpipe);
            decodeThread.start();
        } else {
            Log.e(APP_TAG, "Error: DecodeThread already running");
        }
    }

    public void stopPipe() {
        // Threads can only be suggested to stop
        decodeThread.cont = false;
    }
}
