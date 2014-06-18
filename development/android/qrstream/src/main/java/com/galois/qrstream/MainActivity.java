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
import android.widget.Toast;

import com.galois.qrstream.lib.Constants;
import com.galois.qrstream.lib.Job;
import com.galois.qrstream.lib.ReceiveFragment;
import com.galois.qrstream.lib.SettingsFragment;
import com.galois.qrstream.lib.TransmitFragment;

import org.json.JSONException;
import org.json.JSONObject;

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
            if(startingIntent.getAction().equals(Intent.ACTION_SEND)) {
                try {
                    Job job = buildJobFromIntent(startingIntent);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("job", job);
                    transmitFragment.setArguments(bundle);
                    showFragment(transmitFragment);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, "Unsupported media type.", Toast.LENGTH_LONG).show();
                    finish();
                }
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
            // remove the delayed hideUI
            getWindow().getDecorView().getHandler().removeCallbacksAndMessages(HANDLER_TOKEN_HIDE_UI);
            // Hide now
            hideUI();
            return true;
        }
        if(currentFragment == settingsFragment && id == android.R.id.home) {
            showFragment(lastFragment, true);
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

    private Job buildJobFromIntent(Intent intent) throws IllegalArgumentException {
        String type = intent.getType();
        Bundle extras = intent.getExtras();
        Log.d("qrstream", "** received type "+type);

        String name = "";
        byte[] bytes = null;

        Uri dataUrl = (Uri) intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
        if(dataUrl != null) {
            name = getNameFromURI(dataUrl);
            if (dataUrl.getScheme().equals("content") ||
                dataUrl.getScheme().equals("file")) {
                try {
                    bytes = readFileUri(dataUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(Constants.APP_TAG, "unsupported url: "+dataUrl);
            }
        } else {
            // fall back to content in extras (mime type dependent)
            if(type.equals("text/plain")) {
                String subject = extras.getString(Intent.EXTRA_SUBJECT);
                String text = extras.getString(Intent.EXTRA_TEXT);
                if(subject == null) {
                    bytes = text.getBytes();
                } else {
                    bytes = encodeSubjectAndText(subject, text);
                    type = Constants.MIME_TYPE_TEXT_NOTE;
                }
            }
        }
        return new Job(name, bytes, type);
    }

    private byte[] encodeSubjectAndText(String subject, String text) {
        JSONObject o = new JSONObject();
        try {
            o.put(Intent.EXTRA_SUBJECT, subject);
            o.put(Intent.EXTRA_TEXT, text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o.toString().getBytes();
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

}
