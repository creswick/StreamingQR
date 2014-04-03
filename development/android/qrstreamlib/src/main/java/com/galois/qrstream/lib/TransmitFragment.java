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
    private Transmit transmitter;
    private Job job;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private int count = 0;

    // Updates frame of QR code at regular interval
    private final static int MS_BETWEEN_FRAMES = 800;
    private Handler handleFrameUpdate = new Handler();
    private Runnable runThroughFrames = new Runnable() {
        @Override
        public void run() {
            nextFrame();
            handleFrameUpdate.postDelayed(this,MS_BETWEEN_FRAMES);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transmitter = new Transmit(350,350);
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
        Bundle bundle = getArguments();
        if(bundle != null) {
            job = (Job) bundle.getSerializable("job");
            transmitData(job.getTitle(), job.getData());
            nextFrame();
        }
    }

    private void transmitData(String title, byte[] bytes) {
        Log.d("qrstream", "transmitData title="+title+" byte count="+bytes.length);
        updateUi(title);
        Log.i(Constants.APP_TAG, "Trying to create and transmit QR codes");
        try {
            qrCodes = transmitter.encodeQRCodes(bytes);
            qrCodeIter = qrCodes.iterator();
            count = 0;
            Log.i(Constants.APP_TAG, "Successful creation of QR codes");
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
        String chunkStr = ", chunk: ";
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
        if (qrCodeIter != null) {
            if (qrCodeIter.hasNext()) {
                count++;
                Log.w(Constants.APP_TAG, "Drawing QR Code: " + count);
                updateUI(count);
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
                // If not invoked via "ShareAs" API, then send an random
                // alphanumeric string, to test the transmission.
                if (qrCodeIter == null) {
                    String input = RandomStringUtils.randomAlphanumeric(20);
                    transmitData(input, input.getBytes(Charsets.ISO_8859_1));
                }
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
