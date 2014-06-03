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

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.FrameLayout;

/**
 * Created by donp on 4/8/14.
 */
public class SettingsFragment extends PreferenceFragment {

    private CharSequence previousTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar bar = getActivity().getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        previousTitle = bar.getTitle();
        bar.setTitle(R.string.settings_actionbar_title);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        ActionBar bar = getActivity().getActionBar();
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setTitle(previousTitle);
        getActivity().invalidateOptionsMenu();
    }
}
