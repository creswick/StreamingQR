package com.galois.qrstream.lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
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
    private ProgressBar progressBar;
    private TextView progressText;

    private Camera camera;
    private DecodeThread decodeThread;
    private Activity activity;

    // Help QRlib manage the camera preview requests
    private final CameraManager cameraManager = CameraManager.getInstance();
    private boolean hasSurface = false;

    // Keep track of visibility of Rx fragment so that we can dispose of
    // cancellation messages that are not relevant.  The activity.isFinishing()
    // does not trigger onPause() only onDestroy().
    private boolean isVisible = false;


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
            // Keep track of visibility of Rx fragment so that we can dispose of
            // cancellation messages that are not relevant.  The activity.isFinishing()
            // does not trigger onPause() only onDestroy().
            if(!isVisible || activity.isFinishing()) {
                Log.d(Constants.APP_TAG, "ignoring displayUpdate message. Fragment is not visible.");
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
                        progressBar.setProgress(progressStatus);
                        progressText.setText(""+count+"/"+total+" "+progressStatus+"%");
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

            if(state == State.Fail) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(activity).
                                setMessage(R.string.receive_failedTxt).
                                setPositiveButton(R.string.receive_buttonOkTxt, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
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
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        progressText = (TextView) rootView.findViewById(R.id.progresstext);

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
    public void onPause(){
        Log.d(Constants.APP_TAG, "onPause");
        super.onPause();
        isVisible = false;
        disposeCamera();
    }

    @Override
    public void onResume() {
        Log.d(Constants.APP_TAG, "onResume");
        super.onResume();
        isVisible = true;
        resume();
    }

    private void resume() {
        // Make sure that camera resources had been released
        disposeCamera();
        resetUI();

        /* In some cases, onPause will destroy the camera_window */
        SurfaceView previewSurface = (SurfaceView) rootLayout.findViewWithTag("camera_window");
        if(previewSurface == null) {
            Log.d(Constants.APP_TAG, "Resume: camera_window is null");
            replaceCameraWindow();
        }

        if (hasSurface) {
            // The fragment was paused but not stopped. Happens when power button is pressed
            // instead of exiting through back button, opening settings fragment, or pushing
            // home button. When this happens, the surface still exists and surfaceCreated callback
            // won't be invoked. Therefore, we need to init the camera here.
            initCamera(camera_window.getHolder());
            startPipe(camera.getParameters(), progress, cameraManager);
        }else{
            // Install the callback and wait for surfaceCreated() to init the camera.
            Log.i(Constants.APP_TAG, "ReceiveFragment:onResume: has NO surface.");
            setCameraWindowCallback();
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
        progressBar.setProgress(0);
        progressText.setText("");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Constants.APP_TAG, "surfaceDestroyed");
        hasSurface = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(Constants.APP_TAG, "surfaceCreated");
        if (holder == null) {
            Log.e(Constants.APP_TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        } else {
            if (!hasSurface) {
                hasSurface = true;
                initCamera(holder);
                startPipe(camera.getParameters(), progress, cameraManager);
            }
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
            camera = Camera.open(cameraId);
            camera.setDisplayOrientation(defaultCameraOrientation(cameraId));
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            Camera.Parameters params = camera.getParameters();
            cameraManager.startRunning(camera, new Preview(params.getPreviewSize()));
        } catch (RuntimeException re) {
            // TODO handle this more elegantly.
            Toast.makeText(getActivity(), "Unable to open camera", Toast.LENGTH_LONG).show();
            Log.e(Constants.APP_TAG, "Could not open camera. "+re);
            re.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

        cameraManager.stopRunning();

        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();

        camera = null;
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
            Log.d(Constants.APP_TAG, "startPipe building new decodeThread");
            Camera.Size previewSize = params.getPreviewSize();
            Receive receiveQrpipe = new Receive(previewSize.height,
                                        previewSize.width,
                                        Constants.RECEIVE_TIMEOUT_MS,
                                        progress);

            // Cannot start up this thread yet.  We need to make sure that the
            // camera preview is available for decodeThread to request frames.
            cameraManager.startRunning(camera, new Preview(params.getPreviewSize()));
            decodeThread = new DecodeThread(getActivity(), receiveQrpipe, fm);
            decodeThread.start();
        }
    }
}
