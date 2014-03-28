package com.galois.qrstream;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.galois.qrstream.lib.Constants;
import com.galois.qrstream.lib.Job;
import com.galois.qrstream.lib.ReceiveFragment;
import com.galois.qrstream.lib.TransmitFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends Activity {

    private FragmentManager fragmentManager;
    private ArrayList<Job> jobsList;
    protected ReceiveFragment receiveFragment; // accessed via unittest
    protected TransmitFragment transmitFragment; // accessed via unittest

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        jobsList = new ArrayList();
        fragmentManager = getFragmentManager();
        receiveFragment = new ReceiveFragment();
        transmitFragment = new TransmitFragment(jobsList);

        if (savedInstanceState == null) {
            Intent startingIntent = getIntent();
            Log.d(Constants.APP_TAG, "startingIntent  " + startingIntent.getAction());
            if(startingIntent.getAction() == Intent.ACTION_SEND) {
                Job job = buildJobFromIntent(startingIntent);
                jobsList.add(job);
                showFragment(transmitFragment);
            } else {
                showFragment(receiveFragment);
            }
        }
    }

    private void showFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_receive) {
            showFragment(receiveFragment);
            return true;
        }
        if (id == R.id.action_transmit) {
            showFragment(transmitFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private byte[] readFile(String fileName) {
        File file = new File(fileName);
        byte[] buf = new byte[(int) file.length()];
        try {
            FileInputStream fileStream = new FileInputStream(file);
            fileStream.read(buf);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf;
    }

    private Job buildJobFromIntent(Intent intent) {
        String type = intent.getType();
        Bundle extras = intent.getExtras();
        Log.d("qrstream", "** received type "+type);

        String name = "";
        byte[] bytes = null;

        if(type.equals("image/*")) {
            Uri dataUrl = (Uri) intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
            name = getRealPathFromURI(dataUrl);
            bytes = readFile(name);
        } else if(type.equals("text/plain")) {
            String body = extras.getString(Intent.EXTRA_SUBJECT) +
                          extras.getString(Intent.EXTRA_TEXT);
            bytes = body.getBytes();
        }
        return new Job(name, bytes);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


}
