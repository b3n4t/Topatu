package com.example.topatu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentTopatuSettings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOGTAG = "TopatuLog";
    private OnConfigChanged listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.topatu_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set summary for parameters
        setCurrentValueOnSummary(getPreferenceScreen());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnConfigChanged) {
            listener = (OnConfigChanged) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet fragmentTopatuSettings.OnConfigChanged");
        }
    }

    private void setCurrentValueOnSummary ( Preference pref ) {
        if ( pref instanceof PreferenceScreen ) {
            PreferenceScreen scrPref = (PreferenceScreen) pref;
            for (int x = 0;x<scrPref.getPreferenceCount();x++) {
                setCurrentValueOnSummary(scrPref.getPreference(x));
            }
        } else if ( pref instanceof PreferenceCategory) {
            PreferenceCategory catPref = (PreferenceCategory) pref;
            for (int x = 0;x<catPref.getPreferenceCount();x++) {
                setCurrentValueOnSummary(catPref.getPreference(x));
            }
        } else if ( pref instanceof ListPreference ) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        } else if ( pref instanceof EditTextPreference ) {
            EditTextPreference textPref = (EditTextPreference) pref;
            pref.setSummary(textPref.getText());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);

        setCurrentValueOnSummary(pref);

        if ( listener != null ) {
            Log.v(LOGTAG, "*** config changed. calling MainActivity callback");
            listener.onConfigChanged();
        }
    }

    //
    //
    // Callback interface
    //
    //
    public interface OnConfigChanged {
        public void onConfigChanged ();
    }
}
