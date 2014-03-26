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


/**
 * Created by donp on 2/11/14.
 */
public class TransmitFragment extends Fragment implements View.OnClickListener {

    private TextView dataTitle;
    private ImageView send_window;
    private Button sendButton;
    private final Transmit transmitter;
    private int progress = 0;
    private List<Job> jobsList;
    private Iterable<BitmapImage> qrCodes;

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
        sendButton.setOnClickListener(this);
        return rootView;
    }

    /**
     * Converts from QRlib BitmapImage to Android's Bitmap image typ
     */
    private Bitmap toBitmap(final BitmapImage matrix){
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                bmp.setPixel(x, y, matrix.get(x,y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }

    @Override
    public void onResume(){
        super.onResume();
        sendNextJob();
    }

    public void sendNextJob() {
        if(jobsList.size() > 0) {
            Job job = jobsList.remove(0);
            transmitData(job.getTitle(), job.getData());
        }
    }

    public void transmitData(String title, byte[] bytes) {
        Log.d("qrstream", "transmitData title="+title+" byte count="+bytes.length);
        updateUi(title);
        Iterable<BitmapImage> qrCodes;
        try {
            // TODO Replace encoding of test string, 'foo', with user data
            qrCodes = transmitter.encodeQRCodes(bytes);

            // Debugging output that prints number of generated QR codes
            int count = 0;
            for(BitmapImage i : qrCodes) {
                count++;
            }
            Log.d("qstream", "# codes returned: " + count);

            Log.d("qstream", "Drawing QR code");
            Iterator<BitmapImage> itr = qrCodes.iterator();
            while (itr.hasNext()) {
                Bitmap b = toBitmap(itr.next());
                send_window.setImageDrawable(new BitmapDrawable(getResources(),b));
                itr.remove();
            }
        } catch (TransmitException e) {
            Log.e("qstream", e.getMessage());
        }
    }

    public void updateUi(String title) {
        sendButton.setText("Transmitting");
        sendButton.setEnabled(false);
        dataTitle.setText(title);
    }

    @Override
    public void onClick(View v) {
        Log.d("qstream", "Begin transmission");
        Job job = new Job("Three Bytes", new byte[]{0x66, 0x6F, 0x6F});
        jobsList.add(job);
        sendNextJob();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

}
