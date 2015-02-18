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
public class fragmentMap extends Fragment {

    private static String LOGTAG = "TopatuLog";

    public static fragmentMap  newInstance() {
        fragmentMap f = new fragmentMap();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(LOGTAG, "fragmentMap - onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText("Map goes here");
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(LOGTAG, "fragmentMap - onActivityCreated");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(LOGTAG, "fragmentMap - onPause");
        persistentFriendList.remove("fragmentMap");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOGTAG, "fragmentMap - onResume");
        persistentFriendList.add("fragmentMap");

    }
}
