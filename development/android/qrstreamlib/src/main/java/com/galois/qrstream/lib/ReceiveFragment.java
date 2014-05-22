/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
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
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.widget.TextView;

import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.Receive;
import com.galois.qrstream.qrpipe.State;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Created by donp on 2/11/14.
 */
public class ReceiveFragment extends Fragment implements SurfaceHolder.Callback {

    private SurfaceView camera_window;
    private ViewGroup.LayoutParams camera_window_params;
    private RelativeLayout rootLayout;
    private TorrentBar torrentBar;
    private TextView progressText;

    private Camera camera;
    private DecodeThread decodeThread;
    private Activity activity;

    // LC experimenting with QRlib managing preview requests
    private CameraManager cameraManager = new CameraManager();
    private boolean hasSurface = false;

    /**
     * Handler to process progress updates from the IProgress implementation.
     *
     * This update handler is passed to the Progress object during the UI initialization.
     */
    private Handler displayUpdate = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final Bundle params = msg.getData();
            State state = (State)params.getSerializable("state");
            Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage " + state);
            if(activity.isFinishing()) {
                Log.d(Constants.APP_TAG, "ignoring displayUpdate message. Activity is finishing.");
            } else {
                dispatchState(params, state);
            }
        }

        private void dispatchState(final Bundle params, State state) {
            if(state == State.Intermediate) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int progressStatus = params.getInt("percent_complete");
                        int count = params.getInt("chunk_count");
                        int total = params.getInt("chunk_total");
                        Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage setProgress " + progressStatus);
                        torrentBar.setProgress(progressStatus);
                        progressText.setText(""+count+"/"+total+" "+progressStatus+"%");
                    }
                });
            }
            if(state == State.Final) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        torrentBar.setProgress(torrentBar.getMax());
                        rootLayout.removeView(camera_window);
                    }
                });
            }

            if(state == State.Fail) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(activity).
                                setMessage("Receive failed").
                                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        disposeCamera();
                                        resume();
                                    }
                                }).
                                show();
                    }
                });
            }
        }
    };

    private final Progress progress = new Progress(displayUpdate);

    public ReceiveFragment() {
    }

    /**
     * Called when this fragment is associated with an Activity.
     * Save the activity for use in the displayUpdate handler.
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public @Nullable View onCreateView(@NotNull LayoutInflater inflater,
                                       ViewGroup container,
                                       @NotNull Bundle savedInstanceState) {
        // Will have surface after onSurfaceCreated called
        hasSurface = false;
        View rootView = inflater.inflate(R.layout.receive_fragment, container, false);
        rootLayout = (RelativeLayout)rootView.findViewById(R.id.receive_layout);
        rootLayout.setKeepScreenOn(true);
        camera_window = (SurfaceView)rootLayout.findViewWithTag("camera_window");
        /* remember the camera_window details for rebuilding later */
        camera_window_params = camera_window.getLayoutParams();
        setCameraWindowCallback();
        torrentBar = (TorrentBar) rootView.findViewById(R.id.progressbar);
        progressText = (TextView) rootView.findViewById(R.id.progresstext);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Constants.APP_TAG, "onResume");
        resume();
    }

    private void resume() {
        resetUI();

        /* In some cases, onPause will destroy the camera_window */
        SurfaceView previewSurface = (SurfaceView) rootLayout.findViewWithTag("camera_window");
        if(previewSurface == null) {
            Log.d(Constants.APP_TAG, "onResume camera_window is null");
            replaceCameraWindow();
        }

        try {
            Camera.Parameters params = openCamera();
            startPipe(params, progress, cameraManager);
        }catch (RuntimeException re) {
            // TODO handle this more elegantly.
            Log.e(Constants.APP_TAG, "Could not open camera. "+re);
        }
    }

    /*
     * rebuild the camera window as the first element in rootLayout
     */
    private void replaceCameraWindow() {
        camera_window = new SurfaceView(rootLayout.getContext());
        camera_window.setTag("camera_window");
        rootLayout.addView(camera_window, 0, camera_window_params);
        setCameraWindowCallback();
    }

    private void setCameraWindowCallback() {
        SurfaceHolder camWindowHolder = camera_window.getHolder();
        camWindowHolder.addCallback(this);
    }

    /*
     * Reset the UI elements to an initial state.
     */
    private void resetUI() {
        torrentBar.setProgress(0);
        progressText.setText("");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(Constants.APP_TAG, "onPause");
        disposeCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(Constants.APP_TAG, "surfaceCreated");
        if (holder == null) {
            Log.e (Constants.APP_TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            //initCamera(holder);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(Constants.APP_TAG, "surfaceChanged");
        if(camera == null) {
            Log.e(Constants.APP_TAG, "surfaceChanged but camera is null. WTF?!");
        }else {
            startCameraPreview(holder, camera);
        }
    }

    private void startCameraPreview(SurfaceHolder holder, Camera c) {
        try {
            c.setPreviewDisplay(holder);
            c.startPreview();
            if(decodeThread == null) {
                Log.e(Constants.APP_TAG, "camera preview started, but DecodeThread == null");
            }else{
                if(!decodeThread.isAlive()) {
                    decodeThread.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Constants.APP_TAG, "surfaceDestroyed");
        disposeCamera();
        hasSurface = false;
    }

    private void initCamera (SurfaceHolder surfaceHolder) throws IOException {
        if(surfaceHolder == null) {
            throw new IllegalStateException("No valid SurfaceHolder provided");
        }
        if (camera != null) {
            Log.d(Constants.APP_TAG, "initCamera() while camera already open.");
            return;
        }
        int cameraId = 0;
        camera = Camera.open();
        camera.setPreviewDisplay(surfaceHolder);
        camera.setDisplayOrientation(defaultCameraOrientation(cameraId));
        Camera.Parameters params = camera.getParameters();

    }

    /*
     * Reserve the hardware camera and setup a callback for the preview frames
     */
    private Camera.Parameters openCamera() {
        // TODO Camera.open() returns null if there is no back-facing camera.
        camera = Camera.open();
        setCameraDisplayOrientation(camera);
        Camera.Parameters params = camera.getParameters();
        cameraManager.startRunning(camera, new Preview(params.getPreviewSize()));
        return params;
    }

    private void disposeCamera() {
        if (null == camera) {
            return;
        }

        cameraManager.stopRunning();

        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();

        camera = null;
    }

    public void setCameraDisplayOrientation(android.hardware.Camera camera) {
        int cameraId = 0;
        camera.setDisplayOrientation(defaultCameraOrientation(cameraId));
    }

    /**
     * Calculate the default orientation for the requested camera.
     * Front facing cameras will compensate for the mirror effect.
     *
     * @param cameraId The id of the camera to display.
     * @return Rotation in degrees of the camera display.
     */
    private int defaultCameraOrientation(int cameraId) {
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
        return result;
    }

    /*
     * Create a worker thread for decoding the preview frames
     * using the qrlib receiver.
     */
    public void startPipe(Camera.Parameters params, IProgress progress, CameraManager fm) {
        Log.d(Constants.APP_TAG, "startPipe");
        if(decodeThread != null) {
            if(decodeThread.isAlive()) {
                Log.e(Constants.APP_TAG, "startPipe Error: DecodeThread already running");
            } else {
                Log.d(Constants.APP_TAG, "startPipe dropping dead thread");
                // drop dead thread
                decodeThread = null;
            }
        }

        if(decodeThread == null) {
            Log.d(Constants.APP_TAG, "startPipe starting decodeThread");
            Camera.Size previewSize = params.getPreviewSize();
            Receive receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width,
                                        Constants.RECEIVE_TIMEOUT_MS,
                                        progress);
            decodeThread = new DecodeThread(getActivity(), receiveQrpipe, fm);
        }
    }

}
