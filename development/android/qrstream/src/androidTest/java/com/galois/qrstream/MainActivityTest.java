package com.galois.qrstream;

import android.app.Activity;
import android.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.galois.qrstream.MainActivity;
import com.galois.qrstream.R;

/**
 * Created by donp on 3/3/14.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2 {

    private MainActivity _activity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setActivityInitialTouchMode(false);

        _activity = (MainActivity)getActivity();
    }

    public void testStart() throws Exception {
        FrameLayout rootView = (FrameLayout)_activity.findViewById(R.id.container);
        assertNotNull(rootView);
    }

    public void testFragment() throws Exception {
        assertTrue(_activity.receiveFragment.isAdded());
    }

}
