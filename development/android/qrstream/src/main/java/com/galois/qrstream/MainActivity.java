package com.galois.qrstream;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.gesture.Gesture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.galois.qrstream.lib.Constants;
import com.galois.qrstream.lib.Job;
import com.galois.qrstream.lib.ReceiveFragment;
import com.galois.qrstream.lib.SettingsFragment;
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

public class MainActivity extends CommonActivity implements View.OnTouchListener {

    private static final String HANDLER_TOKEN_HIDE_UI = "hide_ui";
    private static final int HIDE_UI_DELAY_MS = 3000;
    private FragmentManager fragmentManager;
    private Fragment currentFragment, lastFragment;
    protected ReceiveFragment receiveFragment; // accessed via unittest
    protected TransmitFragment transmitFragment; // accessed via unittest
    protected SettingsFragment settingsFragment; // accessed via unittest

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();
        hideUI();

        fragmentManager = getFragmentManager();
        transmitFragment = new TransmitFragment();
        receiveFragment = new ReceiveFragment();
        settingsFragment = new SettingsFragment();

        // Load application's default settings before user opens settings
        // screen because we want Rx and Tx to run with defaults.
        PreferenceManager.setDefaultValues(this,com.galois.qrstream.lib.R.xml.settings, false);

        if (savedInstanceState == null) {
            Intent startingIntent = getIntent();
            Log.d(Constants.APP_TAG, "startingIntent  " + startingIntent.getAction());
            if(startingIntent.getAction() == Intent.ACTION_SEND) {
                Job job = buildJobFromIntent(startingIntent);
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
        showFragment(fragment, false);
    }

    private void showFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Replace null parameter with string only if
        // change to ft.replace(int,Fragment,String)
        ft.replace(R.id.container, fragment);
        if (addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
        lastFragment = currentFragment;
        currentFragment = fragment;
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
            showFragment(settingsFragment, true);
            getWindow().getDecorView().getHandler().removeCallbacksAndMessages(HANDLER_TOKEN_HIDE_UI);
            return true;
        }
        if(currentFragment == settingsFragment && id == android.R.id.home) {
            showFragment(lastFragment, true);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem settingsButton = menu.findItem(R.id.action_settings);
        if(currentFragment == settingsFragment) {
            menu.removeItem(R.id.action_settings);
        }
        return true;
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
        return new Job(name, bytes, type);
    }

    private void setupUI() {
        View rootView = findViewById(R.id.container);
        rootView.setOnTouchListener(this);
    }

    public boolean onTouch(View v, MotionEvent event) {
        showUI();
        return false;
    }

    private void showUI() {
        getActionBar().show();
        Handler windowHandler = getWindow().getDecorView().getHandler();
        getWindow().getDecorView().getHandler().postAtTime(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.hideUI();
            }
        }, HANDLER_TOKEN_HIDE_UI, SystemClock.uptimeMillis() + HIDE_UI_DELAY_MS);
    }

    private void hideUI() {
        getActionBar().hide();
        View rootView = findViewById(R.id.container);
        rootView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
         );
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
