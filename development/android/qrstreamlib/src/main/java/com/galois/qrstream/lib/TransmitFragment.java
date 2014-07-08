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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Iterator;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import com.galois.qrstream.qrpipe.Transmit;
import com.galois.qrstream.qrpipe.TransmitException;
import com.galois.qrstream.image.BitmapImage;

/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends Fragment {

    // Allows easy toggling of background color for debugging frame sizes
    private final boolean DEBUG_COLOR = false;

    // When true, place the labels after the QR codes, otherwise place before image.
    // Trying to make it easier to play around with some of the layouts.
    private final boolean IS_LABEL_AFTER = true;

    // User setting. When true, display QR chunkIds on screen,
    // otherwise hide them to use more screen real estate for QR codes.
    private boolean hasQRCodeLabels = true;

    private TableLayout send_window;
    private Button sendButton;
    private Transmit transmitter;

    // Allows us to step through QR code transmission
    private Iterable<BitmapImage> qrCodes;
    private Iterator<BitmapImage> qrCodeIter;
    private BitmapImage currentQR;

    // Manage shared settings for application
    private SharedPreferences settings;
    private int numQRCodesDisplayed = 1;
    private final OnSharedPreferenceChangeListener settingsChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
                if (key.equalsIgnoreCase("display_labels")) {
                    hasQRCodeLabels = pref.getBoolean(key, true);
                    Log.i(Constants.APP_TAG, "new show QR code label value= " + hasQRCodeLabels);
                } else if (key.equalsIgnoreCase("frame_time")) {
                    // Only the frame rate changed, no need to restart transmission
                    transmitInterval = Integer.parseInt(pref.getString(key, ""));
                    Log.i(Constants.APP_TAG, "new frame time =" + transmitInterval);
                } else if (key.equalsIgnoreCase("frame_population")) {
                    // Save display count so that we can populate the Tx with the
                    // right number of QR codes. No need to restart transmission
                    // since this will be handled onCreateView
                    numQRCodesDisplayed = Integer.parseInt(pref.getString(key, ""));
                    Log.i(Constants.APP_TAG, "new # of QR codes to display =" + numQRCodesDisplayed);
                } else {
                    Log.i(Constants.APP_TAG, "Setting with key changed: " + key
                            + " to value: " + pref.getString(key, "no default"));
                    // When remaining settings (qr_density and error_correction)
                    // get updated, the transmission will need to be restarted.
                    sendJob();
                }
            }
        };

    // Updates frame of QR code at regular interval
    private int transmitInterval = 0; // set once settings are loaded
    private final Handler handleFrameUpdate = new Handler();
    private final Runnable runThroughFrames = new Runnable() {
        @Override
        public void run() {
            displayNextFrame();
            handleFrameUpdate.postDelayed(this,transmitInterval);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        // Context is first available at this lifecycle stage and so
        // fragment can get reference to the preference settings for app.
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
        settings.registerOnSharedPreferenceChangeListener(settingsChangeListener);

        // Setup initial default interval since we have settings
        transmitInterval = Integer.parseInt(settings.getString("frame_time", ""));
        numQRCodesDisplayed = Integer.parseInt(settings.getString("frame_population", "1"));
        hasQRCodeLabels = settings.getBoolean("display_labels", true);

        super.onAttach(activity);
    }

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

        // Determine the row and columns to display based on settings
        int columnCount = (numQRCodesDisplayed <= 2) ? 1 : 2;
        int rowCount = numQRCodesDisplayed / columnCount;

        send_window = (TableLayout) rootView.findViewById(R.id.send_window);
        // TODO Remove after satisfied with layout
        if (DEBUG_COLOR) {
            send_window.setBackgroundColor(Color.CYAN);
        }

        // Setup the layout parameters for each row and column in the table.
        // A row in a table must use the layout parameters from its parent, i.e. TableLayout params
        TableLayout.LayoutParams tableRowLayout = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT, 1.0f);

        // Items in a TableRow must use the layout parameters from its parent, i.e. TableRow params
        TableRow.LayoutParams tableColumnLayout = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT, 1.0f);

        // Create placeholder views for each of the QR codes and labels too.
        for (int r=1; r<=rowCount; r++) {
            TableRow imgRow = new TableRow(getActivity());
            TableRow labelRow = new TableRow(getActivity());

            // The default row layout will set the child width to MATCH_PARENT and
            // the child height to WRAP_CONTENT. Default is okay for labels but will
            // not stretch the images very well. Therefore set the layout for the image row only.
            imgRow.setLayoutParams(tableRowLayout);
            for (int c = 1; c <= columnCount; c++) {

                // Create the text view for each of the QR code labels
                if (hasQRCodeLabels) {
                    TextView textView = new TextView(getActivity());
                    textView.setGravity(Gravity.CENTER_HORIZONTAL);
                    textView.setLayoutParams(tableColumnLayout);
                    labelRow.addView(textView);
                }

                // Create the view where each of the QR codes will get displayed
                ImageView imgView = new ImageView(getActivity());
                // TODO Remove after satisfied with layout
                if (DEBUG_COLOR) {
                    if (r % 4 == 3) {
                        if (c == 1) {
                            imgView.setBackgroundColor(Color.BLUE);
                        } else {
                            imgView.setBackgroundColor(Color.LTGRAY);
                        }
                    } else {
                        if (c == 1) {
                            imgView.setBackgroundColor(Color.LTGRAY);
                        } else {
                            imgView.setBackgroundColor(Color.BLUE);
                        }
                    }
                }

                // Items in a TableRow must use the layout parameters from its parent, i.e. TableRow params
                imgView.setLayoutParams(tableColumnLayout);
                imgRow.addView(imgView);
            }

            if (!hasQRCodeLabels) {
                send_window.addView(imgRow);
            } else {
                if (IS_LABEL_AFTER) {
                    send_window.addView(imgRow);
                    send_window.addView(labelRow);
                } else {
                    send_window.addView(labelRow);
                    send_window.addView(imgRow);
                }
            }
        }

        send_window.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (transmitter == null) {

                    Log.d(Constants.APP_TAG, "onLayoutChange Transmitter created for width " +
                            send_window.getWidth() + " height " + send_window.getHeight());

                    // Calculate the image height and width given the row and column count.
                    // The numRows we divide by two since we added rows for the image labels.
                    int numCols = 1;
                    int numRows = send_window.getChildCount();
                    if (hasQRCodeLabels) {
                        numRows = numRows / 2;
                    }
                    if (numRows >= 1) {
                        TableRow r = (TableRow) send_window.getChildAt(0);
                        numCols = r.getChildCount();
                    }
                    Log.i(Constants.APP_TAG, "onLayoutChange: numRows= " + numRows +
                                             " numCols= " + numCols);

                    int imageWidth = send_window.getWidth() / numCols;
                    int imageHeight = send_window.getHeight() / numRows;
                    Log.i(Constants.APP_TAG, "onLayoutChange: imgWidth= " + imageWidth +
                            " imgHeight= " + imageHeight);

                    // Choose smaller dimension so that we reduce the whitespace of
                    // final image. Otherwise, the taller dimension would just be filled in
                    // with white pixels.
                    int smallerDimension = imageWidth < imageHeight ? imageWidth : imageHeight;
                    transmitter = new Transmit(smallerDimension, smallerDimension);

                    sendJob();
                    Log.d(Constants.TIMING_TAG, "QR dimension: " + smallerDimension);
                }
            }
        });

        sendButton = (Button)rootView.findViewWithTag("send");
        sendButton.setOnClickListener(new CaptureClick());
        return rootView;
    }

    private void sendJob() {
        Bundle bundle = getArguments();
        if(bundle != null) {
            Job job = (Job) bundle.getSerializable("job");
            transmitData(job);
            showInitialFrame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        repeatFrame();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTransmission();
    }

    private void transmitData(Job job) {
        byte[] bytes = job.getData();
        String title = job.getTitle();
        Log.i(Constants.APP_TAG, "Trying to create and transmit QR codes");
        Log.i(Constants.APP_TAG, "transmitData title=" + title + " byte count=" + bytes.length);

        // Retrieve transmission settings before encoding QR codes
        int density = Integer.parseInt(settings.getString("qr_density", "10"));
        ErrorCorrectionLevel ecLevel = Enum.valueOf(ErrorCorrectionLevel.class,
                                                settings.getString("error_correction", "L"));
        Log.i(Constants.APP_TAG, "transmitData density=" + density +
                                 " error correction Level=" + ecLevel);

        try {
            qrCodes = transmitter.encodeQRCodes(job, density, ecLevel);
            qrCodeIter = qrCodes.iterator();
            currentQR = null;
            Log.i(Constants.APP_TAG, "transmitData(), Successful creation of QR codes");

            // Log some performance characteristics at start of job
            logNewJob(title, bytes.length);
            logUserSettings();

        } catch (TransmitException e) {
            Log.e(Constants.APP_TAG, e.getMessage());
        }
    }

    private void displayNextFrame() {
        currentQR = getNextFrame();
        insertFrame(currentQR);
    }

    // Fill up the display with the number of QR codes set in the preferences
    private void showInitialFrame() {
        for (int i=0; i<numQRCodesDisplayed; i++) {
            currentQR = getNextFrame();
            insertFrame(currentQR);
            if (i == 0) {
                logTotalQRCodes(currentQR);
            }
        }
    }

    // Returns the next BitmapImage in the sequence
    // when it is available, otherwise, return null.
    private BitmapImage getNextFrame() {
        if (qrCodes == null) {
            return null;
        }
        // Reset so we can transmit again
        if (qrCodeIter == null || !qrCodeIter.hasNext()) {
            resetQRTransmission();
        }
        return qrCodeIter.next();
    }

    /**
     * Re-draw a frame with QR codes whenever resume from pause
     */
    private void repeatFrame() {
        // When a view is full of QR codes, pauses, and then resumes display
        // this function will not restore multiple QR codes - only the last
        // one that was displayed.
        // Probably better to restore full state but not part of this refactor.
        // TODO: Perhaps save the display state on pause and then restore it on resume
        if (currentQR != null) {
            insertFrame(currentQR);
            for (int i=1; i<numQRCodesDisplayed; i++) {
                currentQR = getNextFrame();
                insertFrame(currentQR);
            }
        }
    }

    private void logTotalQRCodes(BitmapImage qrCode) {
        String chunkStr = qrCode.toString();
        String description = "Total QR codes: ";

        if (chunkStr.contains("of")) {
            // peel off the total chunks from the end
            String[] splitStr = chunkStr.split("of");
            Log.d(Constants.TIMING_TAG, description + splitStr[1]);
        } else {
            // Otherwise there is only one chunk
            Log.d(Constants.TIMING_TAG, description + chunkStr.substring("chunk ".length()));
        }
    }
    /**
     * Draw a frame containing QR codes to the phone. This pushes any other
     * images down to the next location in the view.
     *
     * @param qrCode The image to display on the UI, should contain QR codes.
     */
    private void insertFrame(BitmapImage qrCode) {
        // Push down all of the other frames
        moveImagesDown();

        // Insert frame in the first row and first column of table
        int r = 0;
        int c = 0;

        TableRow imageRow = getImageRow(r);
        ImageView imgView = (ImageView) imageRow.getChildAt(c);

        // Insert an empty frame whenever passed an null BitmapImage
        if (qrCode != null) {
            Bitmap b = toBitmap(qrCode);
            imgView.setImageDrawable(new BitmapDrawable(getResources(), b));
        } else{
            imgView.setImageDrawable(null);

        }
        // Update the label for the QR code too, if desired.
        if (hasQRCodeLabels) {
            TableRow labelRow = getLabelRow(r);
            TextView textView = (TextView) labelRow.getChildAt(c);
            if (qrCode != null) {
                textView.setText(qrCode.toString());
            }else{
                textView.setText(null);
            }
        }

    }

    // Retrieve the image row in an label agnostic way
    private TableRow getImageRow(int r) {
        if(hasQRCodeLabels) {
            if (IS_LABEL_AFTER) {
                return (TableRow) send_window.getChildAt(2*r);
            }
            return (TableRow) send_window.getChildAt(2*r + 1);
        }
        return (TableRow) send_window.getChildAt(r);
    }

    // Retrieve the label row in an order agnostic way
    private TableRow getLabelRow(int r) {
        // If no labels, then no label row
        if(!hasQRCodeLabels) {
            return null;
        }
        if(IS_LABEL_AFTER) {
            return (TableRow) send_window.getChildAt(2*r + 1);
        }
        return (TableRow) send_window.getChildAt(2*r);
    }

    // Returns the number of rows as if there were no labels
    private int getNumRows() {
        int numRows = send_window.getChildCount();
        if (hasQRCodeLabels) {
            return numRows / 2;
        }
        return numRows;
    }

    private void moveImagesDown() {
        Drawable prevImage = null;
        CharSequence prevLabel = null;

        int nRows = getNumRows();
        for (int r = 0; r < nRows; r++) {
            TableRow imageRow = getImageRow(r);
            TableRow labelRow = getLabelRow(r);

            for (int c = 0; c < imageRow.getChildCount(); c++) {
                //Indicates which frame from the stream is being displayed.
                ImageView imgView = (ImageView) imageRow.getChildAt(c);

                // Pop off the current frame
                Drawable tmpImg = imgView.getDrawable();

                // Replace it with previous frame
                imgView.setImageDrawable(prevImage);

                // Set the previous frame to saved frame
                prevImage = tmpImg;

                // Do the same for the labels if there are any
                if(hasQRCodeLabels) {
                    TextView textView = (TextView) labelRow.getChildAt(c);
                    CharSequence tmpText = textView.getText();
                    textView.setText(prevLabel);
                    prevLabel = tmpText;
                }

            }
        }
    }

    /**
     * Restart transmission by resetting QR code iterators
     */
    private void resetQRTransmission() {
        if (qrCodes != null) {
            Log.d(Constants.APP_TAG, "Tx: Restarting transmission");
            qrCodeIter = qrCodes.iterator();
        }
    }
    /**
     * Stop iterating through the QR codes
     */
    private void pauseTransmission() {
        handleFrameUpdate.removeCallbacks(runThroughFrames);
        sendButton.setText(getString(R.string.transmit_sendButtonTxt));
    }
    /*
     * Resume iterating through the QR codes
     */
    private void resumeTransmission() {
        sendButton.setText(getString(R.string.transmit_pauseButtonTxt));
        handleFrameUpdate.post(runThroughFrames);
    }

    private class CaptureClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String buttonTxt = sendButton.getText().toString();
            if(getString(R.string.transmit_sendButtonTxt).equalsIgnoreCase(buttonTxt)) {
                resumeTransmission();
            } else {
                pauseTransmission();
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

    // Output some of the user settings to the performance log
    private void logUserSettings() {
        //  Number of QR codes per frame
        Log.d(Constants.TIMING_TAG, "Number QR codes per frame: " + numQRCodesDisplayed);

        //  Time to display each frame
        Log.d(Constants.TIMING_TAG, "Desired frame display time (ms): " + transmitInterval);

        //  QR version and error correction level
        Log.d(Constants.TIMING_TAG, "QR version:" +
                settings.getString("qr_density", "10"));
        Log.d(Constants.TIMING_TAG, "Error correction level: " +
                settings.getString("error_correction", "L"));

        // Note: Payload bytes per frame comes from QRlib (getPayloadMaxBytes)

    }

    // Output some of the job characteristics to the performance log
    private void logNewJob(String jobTitle, int msgLength) {
        String logNewJob = "New job starting";
        if (!jobTitle.isEmpty()) {
            logNewJob = logNewJob + ", title: " + jobTitle;
        }
        Log.d(Constants.TIMING_TAG, logNewJob);
        Log.d(Constants.TIMING_TAG, "Message length (bytes): " + msgLength);
    }

}
