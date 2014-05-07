package com.galois.qrstream.lib;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by donp on 4/8/14.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }
}
