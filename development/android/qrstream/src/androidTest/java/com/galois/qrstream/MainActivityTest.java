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
