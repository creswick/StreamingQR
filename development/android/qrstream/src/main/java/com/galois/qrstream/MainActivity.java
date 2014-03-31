package com.galois.qrstream;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {

    private FragmentManager fragmentManager;
    private List<Job> jobsList;
    protected ReceiveFragment receiveFragment; // accessed via unittest
    protected TransmitFragment transmitFragment; // accessed via unittest

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        jobsList = new ArrayList<Job>();
        fragmentManager = getFragmentManager();
        transmitFragment = new TransmitFragment();
        receiveFragment = new ReceiveFragment();

        if (savedInstanceState == null) {
            Intent startingIntent = getIntent();
            Log.d(Constants.APP_TAG, "startingIntent  " + startingIntent.getAction());
            if(startingIntent.getAction() == Intent.ACTION_SEND) {
                Job job = buildJobFromIntent(startingIntent);
                transmitFragment = new TransmitFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable("job", job);
                transmitFragment.setArguments(bundle);
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

    private String getNameFromURI(Uri uri) {
        String name;
        Cursor metadata = getContentResolver().query(uri,
                                                     new String[] {OpenableColumns.DISPLAY_NAME},
                                                     null, null, null);
        if(metadata != null && metadata.moveToFirst()) {
            name = metadata.getString(0);
        } else {
            name = uri.getLastPathSegment();
        }
        return name;
    }

    private byte[] readFileUri(Uri uri) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        AssetFileDescriptor fd = contentResolver.openAssetFileDescriptor(uri, "r");
        long fileLength = fd.getLength();
        Log.d("qrstream","fd length "+fileLength);
        DataInputStream istream = new DataInputStream(contentResolver.openInputStream(uri));
        byte[] buf = new byte[(int)fileLength];
        istream.readFully(buf);
        return buf;
    }

    private Job buildJobFromIntent(Intent intent) {
        String type = intent.getType();
        Bundle extras = intent.getExtras();
        Log.d("qrstream", "** received type "+type);

        String name = "";
        byte[] bytes = null;

        Uri dataUrl = (Uri) intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
        if(dataUrl != null) {
            if (dataUrl.getScheme().equals("content")) {
                try {
                    name = getNameFromURI(dataUrl);
                    bytes = readFileUri(dataUrl);
                } catch (IOException e) {
                    // todo: Handle IO error
                    e.printStackTrace();
                }
            }
        } else {
            // fall back to content in extras (mime type dependent)
            if(type.equals("text/plain")) {
                String body = extras.getString(Intent.EXTRA_SUBJECT) +
                              extras.getString(Intent.EXTRA_TEXT);
                bytes = body.getBytes();
            }
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
