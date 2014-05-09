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

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
        getActivity().invalidateOptionsMenu();
    }
}
