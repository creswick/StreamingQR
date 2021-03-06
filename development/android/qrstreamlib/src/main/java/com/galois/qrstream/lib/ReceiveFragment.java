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
import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import com.galois.qrstream.qrpipe.IProgress;
import com.galois.qrstream.qrpipe.State;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Created by donp on 2/11/14.
 */
public class ReceiveFragment extends Fragment implements SurfaceHolder.Callback {

    private Camera camera;

    private SurfaceView camera_window;
    private ViewGroup.LayoutParams camera_window_params;
    private RelativeLayout rootLayout;
    private TorrentBar torrentBar;
    private TextView progressText;
    private View statusFooter;
    private View statusHeader;

    private DecodeThread decodeThread;
    private Activity activity;

    // Help QRlib manage the camera preview requests
    private CameraManager cameraManager;

    // Helps manage drawing qr finder points
    private QRFoundPointsView cameraOverlay;

    // Used to show cancellation message on UI thread
    private Runnable runShowRxFailedDialog;

    private static final class DrawFinderPointsRunnable implements Runnable {
        private final float[] points;
        private QRFoundPointsView cameraOverlay;

        public DrawFinderPointsRunnable(QRFoundPointsView cameraOverlay) {
            this.points = new float[0];
            this.cameraOverlay = cameraOverlay;

        }
        public DrawFinderPointsRunnable(@NotNull float[] points, QRFoundPointsView cameraOverlay) {
            if (points.length % 2 != 0) {
                throw new IllegalArgumentException("Expected QR finder points to have even length");
            }
            this.points = points.clone();
            this.cameraOverlay = cameraOverlay;
        }
        public void run() {
            if (points.length > 0) {
                cameraOverlay.setPoints(points);
                cameraOverlay.setVisibility(View.VISIBLE);
            }else{
                cameraOverlay.setVisibility(View.INVISIBLE);
            }
            cameraOverlay.invalidate();
        }
    }

    /**
     * Handler to process progress updates from the IProgress implementation.
     *
     * This update handler is passed to the Progress object during the UI initialization.
     */
    private final DisplayUpdateHandler displayUpdate = new DisplayUpdateHandler();

    /**
     * We're using a private static class here instead of an anonymous class because the anonymous
     * handler could possibly hold on to encompassing resources in the ReceiveFragment instance.
     *
     * The instance we get by creating a new DisplayUpdateHandler object can only hold onto the
     * torrentBar and progressText objects, which it requires.
     */
    private static class DisplayUpdateHandler extends Handler {
        private TorrentBar torrentBar;
        private TextView progressText;
        private View statusFooter;
        private View statusHeader;
        private QRFoundPointsView cameraOverlay;
        private boolean isCameraOn = false;
        private Runnable runShowRxFailedDialog;

        public void setCameraOn(boolean isCameraOn) {
            this.isCameraOn = isCameraOn;
        }

        public void setupUi(TorrentBar tb, TextView pt,
                            View statusHeader, View statusFooter,
                            QRFoundPointsView cameraOverlay, Runnable runShowRxFailedDialog) {
            this.torrentBar = tb;
            this.progressText = pt;
            this.statusHeader = statusHeader;
            this.statusFooter = statusFooter;
            this.cameraOverlay = cameraOverlay;
            this.runShowRxFailedDialog = runShowRxFailedDialog;
        }

        @Override
        public void handleMessage(Message msg) {
            final Bundle params = msg.getData();

            if (msg.what == R.id.progress_update) {
                State state = (State) params.getSerializable("state");
                Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage " + state);
                dispatchState(params, state);
            } else if (msg.what == R.id.draw_qr_points) {
                float[] points = params.getFloatArray("points");
                Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage, draw_qr_points");
                Log.d(Constants.APP_TAG, "draw_qr_points: pts length=" + points.length);
                if (isCameraOn) {
                    this.post(new DrawFinderPointsRunnable(points, cameraOverlay));
                }else{
                    this.post(new DrawFinderPointsRunnable(cameraOverlay));
                }
            } else {
                Log.w(Constants.APP_TAG, "displayUpdate handler received unknown request");
            }
        }

        private void dispatchState(final Bundle params, State state) {
            switch (state) {
                case Fail:
                    this.post(runShowRxFailedDialog);
                    torrentBar.reset();
                    break;
                case Intermediate:
                    this.post(new Runnable() {
                        @Override
                        public void run() {
                            if (torrentBar != null && progressText != null) {
                                int progressStatus = params.getInt("percent_complete");
                                int count = params.getInt("chunk_count");
                                int total = params.getInt("chunk_total");
                                int cellId = params.getInt("chunk_id");
                                Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage cellReceived " + cellId);
                                if(!torrentBar.isConfigured()) {
                                    // First progress message needs to setup the progress bar
                                    torrentBar.setCellCount(total);
                                    statusHeader.setVisibility(View.VISIBLE);
                                    statusFooter.setVisibility(View.VISIBLE);
                                }
                                torrentBar.cellReceived(cellId);
                                progressText.setText("" + count + "/" + total + " " + progressStatus + "%");
                            }
                        }
                    });
                    break;
                case Final:
                    this.post(new Runnable() {
                        @Override
                        public void run() {
                            if (torrentBar != null && progressText != null) {
                                torrentBar.setComplete();
                                progressText.setText("100%");
                                statusFooter.setVisibility(View.GONE);
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };

    private static class UiHandler extends Handler {
        private ReceiveFragment rf;

        public void setupFragment(ReceiveFragment rf) {
            this.rf = rf;
        }

        @Override
        public void handleMessage(Message msg) {
            final Bundle params = msg.getData();
            rf.resetUI();
        }
    }

    private final UiHandler uiHandle = new UiHandler();

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

    View.OnClickListener cancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            cameraManager.stopRunning();
        }
    };

    @Override
    public @Nullable View onCreateView(@NotNull LayoutInflater inflater,
                                       ViewGroup container,
                                       @NotNull Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.receive_fragment, container, false);
        rootLayout = (RelativeLayout)rootView.findViewById(R.id.receive_layout);
        rootLayout.setKeepScreenOn(true);

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                autoFocus();
                return false;
            }
        });


        /* remember the camera_window details for rebuilding later */
        camera_window = (SurfaceView)rootLayout.findViewById(R.id.camera_window);
        camera_window_params = camera_window.getLayoutParams();
        setCameraWindowCallback();
        cameraOverlay = (QRFoundPointsView) rootView.findViewById(R.id.camera_overlay);
        torrentBar = (TorrentBar) rootView.findViewById(R.id.progressbar);
        progressText = (TextView) rootView.findViewById(R.id.progresstext);
        statusHeader = (ViewGroup)rootLayout.findViewById(R.id.status_overlay);
        statusFooter = (ViewGroup)rootLayout.findViewById(R.id.status_overlay_footer);
        Button cancelButton = (Button)rootLayout.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(cancelListener);
        ImageButton progressCancelButton = (ImageButton) rootView.findViewById(R.id.progressbutton);
        progressCancelButton.setOnClickListener(cancelListener);


        runShowRxFailedDialog = new Runnable () {
            @Override
            public void run() {
                Activity activity = getActivity();
                if(activity != null) {
                    Toast.makeText(activity, R.string.receive_failedTxt, Toast.LENGTH_LONG).show();
                    resetUI();
                    startPipe(progress);
                }
            }
        };

        uiHandle.setupFragment(this);
        displayUpdate.setupUi(torrentBar, progressText, statusHeader, statusFooter, cameraOverlay, runShowRxFailedDialog);
        resetUI();
        return rootView;
    }

    @Override
    public void onStart() {
        // Execution order: onStart() then onResume(), onCreateView() may occur before onStart
        Log.e(Constants.APP_TAG, "ReceiveFragment onStart");
        resetUI();
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.e(Constants.APP_TAG, "ReceiveFragment onStop");

        clearPendingUIMessages();
        super.onStop();
    }

    @Override
    public void onPause(){
        Log.e(Constants.APP_TAG, "onPause with Thread #" + Thread.currentThread().getId());
        // The camera preview cannot be shown until a fully initialized SurfaceHolder exists.
        // We setup/release camera in the SurfaceHolder callbacks (surfaceDestroyed, surfaceCreated).
        // It would be nice if those methods setup the camera fully, opening and releasing the
        // resource.
        // Most of the time, the app lifecycle will trigger the surfaceDestroyed callback.
        // However, pushing the POWER button to exit when the app is running, does not trigger
        // the callback.  We could either,
        //   1) remove SurfaceView ourselves to ensure the callbacks will be run, OR
        //   2) manually track whether we have surface and check that if we have surface
        //      in onResume, that we open camera again.

        // Remove the camera preview surface from display, so that
        // the surface will get destroyed and camera will get released
        if (cameraManager != null) {
            // Expect cameraManager to be null only if camera failed to initialize
            cameraManager.stopRunning();
        }
        camera_window.setVisibility(View.INVISIBLE);
        cameraOverlay.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    public void onResume() {

        // Setting the visibility here will cause the surfaceCreated callback
        // to be invoked prompting the camera to be acquired and DecodeThread to start
        camera_window.setVisibility(View.VISIBLE);
        cameraOverlay.setVisibility(View.VISIBLE);
        super.onResume();
    }

    /*
     * (Re)build the camera window as the first element in rootLayout
     */
    // TODO: Maybe delete this after checking that it is not needed in onResume()
    private void replaceCameraWindow() {
        SurfaceView previewSurface = (SurfaceView)rootLayout.findViewWithTag("camera_window");
        if (previewSurface != null) {
            camera_window = previewSurface;
            camera_window_params = camera_window.getLayoutParams();
        } else {
            // Since we destroy the SurfaceView each time we pause the application
            // we need to rebuild the layout.
            if(camera_window == null) {
                Log.e(Constants.APP_TAG,
                        "Unable to find camera_window view, and camera_window == null");
                camera_window = new SurfaceView(rootLayout.getContext());
                camera_window.setTag("camera_window");
                // TODO discover if this case ever happens? Expect that it doesn't because onCreateView sets up camera_window
                throw new RuntimeException("Unable to find camera_window view, and camera_window == null");
            }
            rootLayout.addView(camera_window, 0, camera_window_params);
        }
        setCameraWindowCallback();
    }

    private void setCameraWindowCallback() {
        SurfaceHolder camWindowHolder = camera_window.getHolder();
        camWindowHolder.addCallback(this);
    }

    /*
     * Reset the UI elements to an initial state.
     */
    public void resetUI() {
        statusHeader.setVisibility(View.GONE);
        statusFooter.setVisibility(View.GONE);

        torrentBar.reset();
        progressText.setText("");

    }

    private void clearPendingUIMessages() {
        // Dispose of UI update messages that are no longer relevant.
        // With 'null' as parameter, it removes all pending messages on UI thread.
        // Ok because camera callbacks are not handled on UI thread anymore.
        // We should be able to run this command to remove the alert messages
        //   displayUpdate.removeCallbacks(runShowRxFailedDialog);
        // but there seems to be some kind of race condition that allows the runnable to continue
        // running.
        displayUpdate.removeCallbacksAndMessages(null);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(Constants.APP_TAG, "surfaceDestroyed");
        disposeCamera();
        clearPendingUIMessages();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(Constants.APP_TAG, "surfaceCreated");
        if (holder == null) {
            Log.e(Constants.APP_TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        } else {
            initCamera(holder);
            startPipe(progress);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(Constants.APP_TAG, "surfaceChanged");
    }

    /*
     * Reserve the hardware camera and setup a callback for the preview frames
     */
    private void initCamera (@NotNull SurfaceHolder surfaceHolder) {
        if (camera != null) {
            Log.d(Constants.APP_TAG, "initCamera() while camera already open.");
            return;
        }

        // TODO How should application behave if we find only front facing camera
        int cameraId = requestCameraId();
        try {
            Log.d(Constants.APP_TAG, "initCamera: Trying to open and initialize camera");
            openCamera(cameraId);
            int rotation = defaultCameraOrientation(cameraId);
            camera.setDisplayOrientation(rotation);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            cameraOverlay.setCameraParameters(camera.getParameters().getPreviewSize(), rotation);
            displayUpdate.setCameraOn(true);
            autoFocus();

        } catch (RuntimeException re) {
            // TODO handle this more elegantly.
            Toast.makeText(getActivity(), "Unable to open camera", Toast.LENGTH_LONG).show();
            Log.e(Constants.APP_TAG, "Could not open camera. " + re);
            re.printStackTrace();
            displayUpdate.setCameraOn(false);
        } catch (IOException e) {
            e.printStackTrace();
            displayUpdate.setCameraOn(false);
        }
    }
    private CameraHandlerThread mThread = null;
    private void openCamera(int cameraId) {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera(cameraId);
        }
    }

    // CameraHandlerThread is not using the looper of the main UI thread.
    // and so it does not need to be static handler.
    private class CameraHandlerThread extends HandlerThread {
        private final Handler mHandler;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
            Log.e(Constants.APP_TAG, "CameraHandlerThread Id# " + this.getId());
        }

        public void openCamera(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    camera = getCameraInstance(cameraId);
                    notifyCameraOpened();
                }
            });

            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(Constants.APP_TAG, "wait was interrupted");
            }
        }

        private synchronized void notifyCameraOpened() {
            notify();
        }

        private Camera getCameraInstance(int cameraId) {
            Camera c = null;
            try {
                c = Camera.open(cameraId);
            }catch (RuntimeException e) {
                // TODO handle this more elegantly.
                Log.e(Constants.APP_TAG, "failed to open camera");
            }
            return c;
        }
    }

    /*
     * Returns the id of the first back facing camera if found.
     * Otherwise, it returns the id of the first camera found.
     */
    private int requestCameraId() {
        int cameraId = 0;
        int camerasOnPhone = Camera.getNumberOfCameras();
        for (int i=0; i < camerasOnPhone; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private void disposeCamera() {
        if (null == camera) {
            return;
        }

        // Alerts DrawFinderPointsRunnable that camera is not on and no points can be shown
        displayUpdate.setCameraOn(false);

        // Wait for camera to handle queue of PreviewCallback requests
        // so that the camera can released safely
        mThread.quit(); // should this be interrupted()? (Investigate if we see issues relating to camera shutdown / cleanup?)
        mThread = null;


        camera.stopPreview();
        camera.release();

        camera = null;

        // Dispose of any UI update messages that are no longer relevant.
        clearPendingUIMessages();
    }


    /**
     * Calculate the default orientation for the requested camera.
     * Front facing cameras will compensate for the mirror effect.
     *
     * @param cameraId The id of the camera to display.
     * @return Rotation in degrees of the camera display.
     */
    private int defaultCameraOrientation(int cameraId) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
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
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public void autoFocus() {
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                // Do nothing. Let the user notice the image is in focus
            }
        });
    }
    /*
     * Create a worker thread for decoding the preview frames
     * using the qrlib receiver.
     */
    public void startPipe(IProgress progress) {
        Log.d(Constants.APP_TAG, "startPipe");
        if(decodeThread != null) {
            if(decodeThread.isAlive()) {
                Log.e(Constants.APP_TAG, "startPipe Error: DecodeThread already running");
                Log.e(Constants.APP_TAG, "startPipe Error: DecodeThread state= " + decodeThread.getState());
            } else {
                Log.d(Constants.APP_TAG, "startPipe dropping dead thread");
                // drop dead thread
                decodeThread = null;
            }
        }

        if(decodeThread == null && camera != null) {
            Log.d(Constants.APP_TAG, "startPipe building new decodeThread");

            // If we get this far, the camera preview is available for
            // decodeThread to begin requesting frames.  Make sure that
            // the decodeThread has a new instance of the cameraManager so that it starts in the
            // running state.
            if (cameraManager == null || !cameraManager.isRunning()) {
                cameraManager = new CameraManager(camera);
            }
            decodeThread = new DecodeThread(this.getActivity(), progress, cameraManager, uiHandle);
            decodeThread.start();
        }
    }

}
