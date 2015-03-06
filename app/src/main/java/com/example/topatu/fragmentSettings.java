package com.example.topatu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Ixa on 18/02/2015.
 */
public class fragmentSettings extends Fragment {

    private static String LOGTAG = "TopatuLog";

    public static fragmentSettings  newInstance() {
        fragmentSettings f = new fragmentSettings();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "fragmentSettings - onCreateView"); }
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText("Settings goes here");
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "fragmentSettings - onActivityCreated"); }
    }

    @Override
    public void onPause() {
        super.onPause();
        if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "fragmentSettings - onPause"); }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "fragmentSettings - onResume"); }
    }
}
