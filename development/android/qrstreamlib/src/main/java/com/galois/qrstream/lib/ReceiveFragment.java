package com.galois.qrstream.lib;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.view.ViewGroup;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.State;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by donp on 2/11/14.
 */
public class ReceiveFragment extends Fragment implements SurfaceHolder.Callback {

    private SurfaceView camera_window;
    private View rootView;
    private RelativeLayout rootLayout;
    private ProgressBar progressBar;
    private Handler progressHandler = new Handler();

    private Camera camera;
    private final ArrayBlockingQueue frameQueue = new ArrayBlockingQueue<YuvImage>(1);
    private Receive receiveQrpipe;
    private DecodeThread decodeThread;
    private final Progress progress = new Progress();

    public ReceiveFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.receive_fragment, container, false);
        rootLayout = (RelativeLayout)rootView.findViewById(R.id.receive_layout);
        rootLayout.setKeepScreenOn(true);
        camera_window = (SurfaceView)rootView.findViewById(R.id.camera_window);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        camera = Camera.open();
        setCameraDisplayOrientation(camera);
        Camera.Parameters params = camera.getParameters();
        Preview previewCallback = new Preview(frameQueue, params.getPreviewSize());
        camera.setPreviewCallback(previewCallback);
        camera_window.getHolder().addCallback(this);
        DisplayUpdate displayUpdate = new DisplayUpdate(getActivity());
        progress.setStateHandler(displayUpdate);
        startPipe(params, progress);
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
        camera.setPreviewCallback(null);
        camera.release();
    }

    public void setCameraDisplayOrientation(android.hardware.Camera camera) {
        int cameraId = 0;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static class CaptureClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Log.d("qstream", "Capture Pushed");
        }
    }

    public void startPipe(Camera.Parameters params, IProgress progress) {
        if(decodeThread != null) {
            if(decodeThread.isAlive()) {
                Log.e(Constants.APP_TAG, "Error: DecodeThread already running");
            } else {
                // drop dead thread
                decodeThread = null;
            }
        }

        if(decodeThread == null) {
            Camera.Size previewSize = params.getPreviewSize();
            receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width,
                                        Constants.RECEIVE_TIMEOUT_MS,
                                        progress);
            decodeThread = new DecodeThread(receiveQrpipe, frameQueue);
            decodeThread.start();
        }
    }

    public void stopPipe() {
        // todo: notify the qr code receiver to stop
    }

    public class DisplayUpdate extends Handler {
        private final Activity activity;

        public DisplayUpdate(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            final Bundle params = msg.getData();
            State state = (State)params.getSerializable("state");
            Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage " + state);

            if(state == State.Intermediate) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int progressStatus = params.getInt("percent_complete");
                        Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage setProgress " + progressStatus);
                        progressBar.incrementProgressBy(10);
                    }
                });
            }

            if(state == State.Final) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopPipe();
                        rootLayout.removeView(camera_window);
                    }
                });
            }
        }
    }
}
