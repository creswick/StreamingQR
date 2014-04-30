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
import android.widget.LinearLayout;
import android.view.ViewGroup;

import com.galois.qrstream.image.YuvImage;
import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.State;
import com.google.common.collect.Queues;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by donp on 2/11/14.
 */
public class ReceiveFragment extends Fragment implements SurfaceHolder.Callback {

    private SurfaceView camera_window;

    private RelativeLayout rootLayout;
    private ProgressBar progressBar;

    private LinearLayout ll;
    private Camera camera;
    private Receive receiveQrpipe;
    private final BlockingQueue<YuvImage> frameQueue = Queues.newArrayBlockingQueue(1);
    private DecodeThread decodeThread;

    /**
     * Handler to process progress updates from the IProgress implementation.
     *
     * This update handler is passed to the Progress object during the UI initialization.
     */
    private Handler displayUpdate = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Activity activity = ReceiveFragment.this.getActivity();

            final Bundle params = msg.getData();
            State state = (State)params.getSerializable("state");
            Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage " + state);

            if(state == State.Intermediate) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int progressStatus = params.getInt("percent_complete");
                        Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage setProgress " + progressStatus);
                        progressBar.setProgress(progressStatus);
                    }
                });
            }

            if(state == State.Final) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(progressBar.getMax());
                        rootLayout.removeView(camera_window);
                    }
                });
            }
        }
    };

    private final Progress progress = new Progress(displayUpdate);

    public ReceiveFragment() {
    }

    @Override
    public @Nullable View onCreateView(@NotNull LayoutInflater inflater,
                                       ViewGroup container,
                                       @NotNull Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.receive_fragment, container, false);
        rootLayout = (RelativeLayout)rootView.findViewById(R.id.receive_layout);
        rootLayout.setKeepScreenOn(true);

        camera_window = (SurfaceView)rootView.findViewById(R.id.camera_window);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();

        try {
            resetUI();
            Camera.Parameters params = openCamera();
            startPipe(params, progress);
        }catch (RuntimeException re) {
            // TODO handle this more elegantly.
            Log.e(Constants.APP_TAG, "Could not open camera.");
        }
    }

    /*
     * Reset the UI elements to an initial state.
     */
    private void resetUI() {
        progressBar.setProgress(0);
    }

    /*
     * Reserve the hardware camera and setup a callback for the preview frames
     */
    private Camera.Parameters openCamera() {
        // TODO Camera.open() returns null if there is no back-facing camera.
        camera = Camera.open();
        setCameraDisplayOrientation(camera);
        Camera.Parameters params = camera.getParameters();
        Preview previewCallback = new Preview(frameQueue, params.getPreviewSize());
        camera.setPreviewCallback(previewCallback);
        camera_window.getHolder().addCallback(this);
        return params;
    }

    @Override
    public void onPause(){
        super.onPause();

        disposeCamera();
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
        disposeCamera();
    }

    private void disposeCamera() {
        if (null == camera) {
            return;
        }
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();

        camera = null;
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

    /*
     * Create a worker thread for decoding the preview frames
     * using the qrlib receiver.
     */
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
            Receive receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width,
                                        Constants.RECEIVE_TIMEOUT_MS,
                                        progress);
            decodeThread = new DecodeThread(getActivity(), receiveQrpipe, frameQueue);
            decodeThread.start();
        }
    }

}
