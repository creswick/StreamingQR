package com.galois.qrstream.lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
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

    // Need static references so progress bar can be updated from static UI handler.
    private static ProgressBar progressBar;
    private static TextView progressText;

    // Need static referneces for handler to process camera messages
    // off the UI thread.
    private static Camera camera;

    private SurfaceView camera_window;
    private ViewGroup.LayoutParams camera_window_params;
    private RelativeLayout rootLayout;

    private DecodeThread decodeThread;
    private Activity activity;

    // Help QRlib manage the camera preview requests
    private CameraManager cameraManager;

    // Used to show cancellation message on UI thread
    // The dialog is setup in onCreateView since fragment context is available
    private static AlertDialog alertDialog;
    private static final Runnable runShowRxFailedDialog = new Runnable () {
        @Override
        public void run() {
            Log.e(Constants.APP_TAG, "About to show failed alert message!");
            if (alertDialog != null) {
                alertDialog.show();
            }
        }
    };

    /**
     * Handler to process progress updates from the IProgress implementation.
     *
     * This update handler is passed to the Progress object during the UI initialization.
     */
    private static final Handler displayUpdate = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == R.id.progress_update) {
                final Bundle params = msg.getData();
                State state = (State) params.getSerializable("state");
                Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage " + state);
                dispatchState(params, state);
            } else {
                Log.w(Constants.APP_TAG, "displayUpdate handler received unknown request");
            }
        }

        private void dispatchState(final Bundle params, State state) {
            switch (state) {
                case Fail:
                    // Add a slight delay so messages from this handler can
                    // be removed when onPause() but also allow user to see them
                    // whenever cancel button is pressed.
                    this.postDelayed(runShowRxFailedDialog, 100);
                    break;
                case Intermediate:
                    this.post(new Runnable() {
                        @Override
                        public void run() {
                            if (progressBar != null && progressText != null) {
                                int progressStatus = params.getInt("percent_complete");
                                int count = params.getInt("chunk_count");
                                int total = params.getInt("chunk_total");
                                Log.d(Constants.APP_TAG, "DisplayUpdate.handleMessage setProgress " + progressStatus);
                                progressBar.setProgress(progressStatus);
                                progressText.setText("" + count + "/" + total + " " + progressStatus + "%");
                            }
                        }
                    });
                    break;
                case Final:
                    this.post(new Runnable() {
                        @Override
                        public void run() {
                            if (progressBar != null && progressText != null) {
                                progressBar.setProgress(progressBar.getMax());
                                progressText.setText("100%");
                            }
                        }
                    });
                    break;
                default:
                    break;
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
        View rootView = inflater.inflate(R.layout.receive_fragment, container, false);
        rootLayout = (RelativeLayout)rootView.findViewById(R.id.receive_layout);
        rootLayout.setKeepScreenOn(true);
        /* remember the camera_window details for rebuilding later */
        camera_window = (SurfaceView)rootLayout.findViewById(R.id.camera_window);
        camera_window_params = camera_window.getLayoutParams();
        setCameraWindowCallback();

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        progressText = (TextView) rootView.findViewById(R.id.progresstext);

        // Setup the alert dialog in case we need it to report Rx errors to the user.
        alertDialog = new AlertDialog.Builder(activity).
                setMessage(R.string.receive_failedTxt).
                setPositiveButton(R.string.receive_buttonOkTxt,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.e(Constants.APP_TAG, "AlertDialog OK pressed, about to resume()");
                                resetUI();
                                startPipe(progress);
                            }
                }).create();

        // TODO We'll want to apply the same listener to the cancel button in ticket-49
        ImageButton progressButton = (ImageButton) rootView.findViewById(R.id.progressbutton);
        progressButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraManager.stopRunning();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        // Execution order: onStart() then onResume(), onCreateView() may occur before onStart
        Log.e(Constants.APP_TAG, "onStart");
        resetUI();
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.e(Constants.APP_TAG, "onStop");

        // Dispose of UI update messages that are no longer relevant.
        // With 'null' as parameter, it removes all pending messages on UI thread.
        // Ok because camera callbacks are not handled on UI thread anymore.
        // We should be able to run this command to remove the alert dialog
        //   displayUpdate.removeCallbacks(runShowRxFailedDialog);
        // However, it's causing the alert dialog to show whenever we navigate to
        // the settings fragment.
        displayUpdate.removeCallbacksAndMessages(null);
        alertDialog.dismiss();
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
        cameraManager.stopRunning();
        camera_window.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    public void onResume() {

        // TODO Check with donp to figure out if this is necessary? I think it's no longer needed.
        /* In some cases, onPause will destroy the camera_window. This reestablishes it. */
        SurfaceView previewSurface = (SurfaceView) rootLayout.findViewWithTag("camera_window");
        if(previewSurface == null) {
            Log.d(Constants.APP_TAG, "Resume: camera_window is null");
            throw new RuntimeException("TODO: make call to replaceCameraWindow() here");
            //replaceCameraWindow();
        }
        // Setting the visibility here will cause the surfaceCreated callback
        // to be invoked prompting the camera to be acquired and DecodeThread to start
        camera_window.setVisibility(View.VISIBLE);
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
    private void resetUI() {
        progressBar.setProgress(0);
        progressText.setText("");

        if (alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    private void clearPendingAlertMessages() {
        // Dispose of cancellation messages that are no longer relevant.
        displayUpdate.removeCallbacks(runShowRxFailedDialog);
        alertDialog.dismiss();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(Constants.APP_TAG, "surfaceDestroyed");
        disposeCamera();
        clearPendingAlertMessages();
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
            camera.setDisplayOrientation(defaultCameraOrientation(cameraId));
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (RuntimeException re) {
            // TODO handle this more elegantly.
            Toast.makeText(getActivity(), "Unable to open camera", Toast.LENGTH_LONG).show();
            Log.e(Constants.APP_TAG, "Could not open camera. "+re);
            re.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
    // and so it does not need to be static handler, but being static doesn't hurt.
    private static class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

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
            }
            catch (InterruptedException e) {
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
            }
            catch (RuntimeException e) {
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

        // Wait for camera to handle queue of PreviewCallback requests
        // so that the camera can released safely
        mThread.quit(); // should this be interrupted()? (Investigate if we see issues relating to camera shutdown / cleanup?)
        mThread = null;

        camera.stopPreview();
        camera.release();

        camera = null;

        // Dispose of cancellation messages that are no longer relevant.
        displayUpdate.removeCallbacks(runShowRxFailedDialog);
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

        if(decodeThread == null) {
            Log.d(Constants.APP_TAG, "startPipe building new decodeThread");

            // If we get this far, the camera preview is available for
            // decodeThread to begin requesting frames.  Make sure that
            // the decodeThread has a new instance of the cameraManager so that it starts in the
            // running state.
            if (cameraManager == null || !cameraManager.isRunning()) {
                cameraManager = new CameraManager(camera);
            }
            decodeThread = new DecodeThread(getActivity(), progress, cameraManager);
            decodeThread.start();
        }
    }
}
