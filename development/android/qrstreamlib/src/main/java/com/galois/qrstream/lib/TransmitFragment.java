package com.galois.qrstream.lib;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
    private Transmit transmitter;
    private Job job;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private int count = 0;

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
        Log.d("qrstream", "transmitData title=" + title + " byte count=" + bytes.length);
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

    private void updateUi(String title) {
        sendButton.setText("Next frame");
        dataTitle.setText(title);
    }

    private void nextFrame() {
        if (qrCodeIter == null || !qrCodeIter.hasNext()) {
            // Reset so we can transmit again
            Log.w(Constants.APP_TAG, "nextFrame resetting iterator");
            qrCodeIter = qrCodes.iterator();
            count = 0;
        }

        count++;
        Log.w(Constants.APP_TAG, "Drawing QR Code: " + count);
        Bitmap b = toBitmap(qrCodeIter.next());
        send_window.setImageDrawable(new BitmapDrawable(getResources(), b));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public class CaptureClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            nextFrame();
        }
    }

    /**
     * Converts from QRlib BitmapImage to Android's Bitmap image type
     */
    private Bitmap toBitmap(final BitmapImage matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
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
