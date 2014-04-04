package com.galois.qrstream.lib;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Iterator;

import com.galois.qrstream.qrpipe.Transmit;
import com.galois.qrstream.qrpipe.TransmitException;
import com.galois.qrstream.image.BitmapImage;
import com.google.common.base.Charsets;
import org.apache.commons.lang.RandomStringUtils;


/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends Fragment {

    private TextView dataTitle;
    private ImageView send_window;
    private Button sendButton;
    private Transmit transmitter;
    private Job job;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private int count = 0;

    // Updates frame of QR code at regular interval
    private final Handler handleFrameUpdate = new Handler();
    private final Runnable runThroughFrames = new Runnable() {
        @Override
        public void run() {
            nextFrame();
            handleFrameUpdate.postDelayed(this,Constants.TRANSMIT_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transmit_fragment, container, false);

        LinearLayout ll = (LinearLayout)rootView.findViewById(R.id.transmit_layout);
        ll.setKeepScreenOn(true);

        send_window = (ImageView)rootView.findViewById(R.id.send_window);
        send_window.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (transmitter == null) {
                    Log.d(Constants.APP_TAG, "onLayoutChange Transmitter created for width " +
                            send_window.getWidth() + " height " + send_window.getHeight());
                    transmitter = new Transmit(send_window.getWidth(), send_window.getHeight());
                    sendJob();
                }
            }
        });

        dataTitle = (TextView)rootView.findViewById(R.id.data_title);
        sendButton = (Button)rootView.findViewWithTag("send");
        sendButton.setOnClickListener(new CaptureClick());
        return rootView;
    }

    private void sendJob() {
        Bundle bundle = getArguments();
        if(bundle != null) {
            job = (Job) bundle.getSerializable("job");
            transmitData(job.getTitle(), job.getData());
            nextFrame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void transmitData(String title, byte[] bytes) {
        Log.i(Constants.APP_TAG, "Trying to create and transmit QR codes");
        Log.i(Constants.APP_TAG, "transmitData title=" + title + " byte count=" + bytes.length);
        updateUi(title);
        try {
            qrCodes = transmitter.encodeQRCodes(bytes);
            qrCodeIter = qrCodes.iterator();
            count = 0;
            Log.i(Constants.APP_TAG, "transmitData(), Successful creation of QR codes");
        } catch (TransmitException e) {
            Log.e(Constants.APP_TAG, e.getMessage());
        }
    }

    private void updateUi(String title) {
        dataTitle.setText(title);
    }

    // Update the title with the QR chunkId being displayed.
    // Imagine it will be most useful for debugging.
    private void updateUI(int chunkId ) {
        String title = dataTitle.getText().toString();
        String chunkStr = getString(R.string.transmit_chunkTxt);
        int index = title.indexOf(chunkStr);
        String newTitle;
        if (index >= 0) {
            newTitle = title.substring(0, index) + chunkStr + chunkId;
        }else{
            newTitle = title + chunkStr + chunkId;
        }
        updateUi(newTitle);
    }

    private void nextFrame() {
        // Doesn't make sense to go to next frame if there are no qrCodes
        if (qrCodes == null) {
            return;
        }
        // Reset so we can transmit again
        if (qrCodeIter == null || !qrCodeIter.hasNext()) {
            Log.w(Constants.APP_TAG, "nextFrame resetting iterator");
            qrCodeIter = qrCodes.iterator();
            count = 0;
        }
        count++;
        Log.w(Constants.APP_TAG, "Drawing QR Code: " + count);
        Bitmap b = toBitmap(qrCodeIter.next());
        send_window.setImageDrawable(new BitmapDrawable(getResources(), b));
        updateUI(count);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private class CaptureClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String buttonTxt = sendButton.getText().toString();
            if(getString(R.string.transmit_sendButtonTxt).equalsIgnoreCase(buttonTxt)) {
                // If not invoked via "ShareAs" API, then send an random
                // alphanumeric string, to test the transmission.
                if (qrCodeIter == null) {
                    String input = RandomStringUtils.randomAlphanumeric(20);
                    transmitData(input, input.getBytes(Charsets.ISO_8859_1));
                }
                if (qrCodeIter != null) {
                    sendButton.setText(getString(R.string.transmit_pauseButtonTxt));
                    handleFrameUpdate.post(runThroughFrames);
                }
            } else {
                sendButton.setText(getString(R.string.transmit_sendButtonTxt));
                handleFrameUpdate.removeCallbacks(runThroughFrames);
            }
        }
    }

    /**
     * Converts from QRlib BitmapImage to Android's Bitmap image type
     * Note: This conversion consumes the majority of the runtime
     * when width and height are large.
     * TODO: Move this out of main UI thread?
     */
    private Bitmap toBitmap(final BitmapImage matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x,y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return bmp;
    }
}
