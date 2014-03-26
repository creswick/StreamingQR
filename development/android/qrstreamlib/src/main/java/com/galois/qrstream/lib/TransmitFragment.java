package com.galois.qrstream.lib;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import java.util.Iterator;

import com.galois.qrstream.qrpipe.Transmit;
import com.galois.qrstream.qrpipe.TransmitException;
import com.galois.qrstream.image.BitmapImage;
import com.google.common.base.Charsets;
import org.apache.commons.lang.RandomStringUtils;


/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends Fragment implements Constants {

    private ImageView send_window;
    private Button sendButton;
    private final Transmit transmitter;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private int count = 0;

    public TransmitFragment() {
        transmitter = new Transmit(350, 350);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transmit_fragment, container, false);

        send_window = (ImageView) rootView.findViewById(R.id.send_window);
        sendButton = (Button) rootView.findViewWithTag("send");
        sendButton.setOnClickListener(new CaptureClick());
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(APP_TAG, "Trying to create and transmit QR codes");
        try {
            // TODO Replace encoding of test strings with user data
            qrCodes = encodeRandomTestData();
            qrCodeIter = qrCodes.iterator();
            count = 0;
            Log.i(APP_TAG, "Successful creation of QR codes");
        } catch (TransmitException e) {
            Log.e(APP_TAG, e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public class CaptureClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(qrCodeIter != null) {
                if(qrCodeIter.hasNext()) {
                    count++;
                    Log.w(APP_TAG, "Drawing QR Code: " + count);
                    Bitmap b = toBitmap(qrCodeIter.next());
                    send_window.setImageDrawable(new BitmapDrawable(getResources(), b));
                }else{
                    // Reset so we can transmit again
                    qrCodeIter = qrCodes.iterator();
                    count = 0;
                }
            }

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
        Log.w(APP_TAG, "About to encode: " + input);
        qrCodes = transmitter.encodeQRCodes(input.getBytes(Charsets.ISO_8859_1));

        // Debugging output that prints number of generated QR codes
        int qrCount = 0;
        for (BitmapImage i : qrCodes) {
            qrCount++;
        }
        Log.w(APP_TAG, "# codes generated: " + qrCount);
        return qrCodes;
    }
}
