package com.galois.qrstream.lib;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

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
    private final Transmit transmitter;
    private int progress = 0;
    private List<Job> jobsList;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private int count = 0;

    // Updates frame of QR code at regular interval
    private final static int MS_BETWEEN_FRAMES = 1000;
    private Handler handleFrameUpdate = new Handler();
    private Runnable runThroughFrames = new Runnable() {
        @Override
        public void run() {
            nextFrame();
            handleFrameUpdate.postDelayed(this,MS_BETWEEN_FRAMES);
        }
    };

    public TransmitFragment(List<Job> jobsList) {
        transmitter = new Transmit(350,350);
        this.jobsList = jobsList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transmit_fragment, container, false);

        send_window = (ImageView)rootView.findViewById(R.id.send_window);
        dataTitle = (TextView)rootView.findViewById(R.id.data_title);
        sendButton = (Button)rootView.findViewWithTag("send");
        sendButton.setOnClickListener(new CaptureClick());
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        sendNextJob();
    }

     public void sendNextJob() {
         if(jobsList.size() > 0) {
             Job job = jobsList.remove(0);
             transmitData(job.getTitle(), job.getData());
             nextFrame();
         }
    }

    public void transmitData(String title, byte[] bytes) {
        Log.d("qrstream", "transmitData title="+title+" byte count="+bytes.length);
        updateUi(title);
        Log.i(Constants.APP_TAG, "Trying to create and transmit QR codes");
        try {
            // TODO Replace encoding of test strings with user data
            //qrCodes = encodeRandomTestData();
            qrCodes = transmitter.encodeQRCodes(bytes);
            qrCodeIter = qrCodes.iterator();
            count = 0;
            Log.i(Constants.APP_TAG, "Successful creation of QR codes");
        } catch (TransmitException e) {
            Log.e(Constants.APP_TAG, e.getMessage());
        }
    }

    public void updateUi(String title) {
        dataTitle.setText(title);
    }

    private void nextFrame() {
        if (qrCodeIter != null) {
            if (qrCodeIter.hasNext()) {
                count++;
                Log.w(Constants.APP_TAG, "Drawing QR Code: " + count);
                Bitmap b = toBitmap(qrCodeIter.next());
                send_window.setImageDrawable(new BitmapDrawable(getResources(), b));
            } else {
                // Reset so we can transmit again
                qrCodeIter = qrCodes.iterator();
                count = 0;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public class CaptureClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if("Send".equalsIgnoreCase(sendButton.getText().toString())) {
                if (qrCodeIter != null) {
                    sendButton.setText("Pause");
                    handleFrameUpdate.post(runThroughFrames);
                }
            } else {
                sendButton.setText("Send");
                handleFrameUpdate.removeCallbacks(runThroughFrames);
            }
        }
    }

    /**
     * Converts from QRlib BitmapImage to Android's Bitmap image type
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

    /**
     * Make more dynamic test string using Apache commons random string generator
     */
    private Iterable<BitmapImage> encodeRandomTestData() throws TransmitException {
        String input = RandomStringUtils.randomAlphanumeric(20);
        Log.w(Constants.APP_TAG, "About to encode: " + input);
        qrCodes = transmitter.encodeQRCodes(input.getBytes(Charsets.ISO_8859_1));

        // Debugging output that prints number of generated QR codes
        int qrCount = 0;
        for (BitmapImage i : qrCodes) {
            qrCount++;
        }
        Log.w(Constants.APP_TAG, "# codes generated: " + qrCount);
        return qrCodes;
    }
}
