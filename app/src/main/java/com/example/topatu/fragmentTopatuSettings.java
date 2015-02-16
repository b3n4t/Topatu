package com.example.topatu;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentTopatuSettings extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.topatu_settings);
    }

    public static fragmentTopatuSettings  newInstance() {
        fragmentTopatuSettings f = new fragmentTopatuSettings();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }


}
