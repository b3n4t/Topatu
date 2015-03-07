package com.example.topatu;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

/**
 * Created by Ixa on 18/02/2015.
 */
public class fragmentMap extends Fragment implements OnMapReadyCallback, persistentFriends.friendEvents, AdapterView.OnItemSelectedListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener {

    private static String LOGTAG = "TopatuLog";
    private persistentFriends friendData = null;
    private boolean centerOnFriend = false;
    private FrameLayout frame = null;
    private MapView myMapView = null;
    private GoogleMap myMap = null;
    private Marker friendMarker = null;
    private Circle friendCircle = null;
    private miataruFriend activeFriend = null;
    private long activeFriendTimeStamp = 0;
    private Spinner friendName = null;
    private adapterFriendArray friendAdapter;



    public static fragmentMap  newInstance() {
        fragmentMap f = new fragmentMap();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "fragmentMap - onCreateView"); }
        //View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        //textView.setText("Map goes here");
        //return rootView;

        //frame = (FrameLayout)inflater.inflate(R.layout.fragment_map, container, false);
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        MapsInitializer.initialize(inflater.getContext());

        myMapView = ((MapView)view.findViewById(R.id.map_googlemap));

        // set adapter to spinner
        friendName = (Spinner)view.findViewById(R.id.map_friend_show);
        friendName.setOnItemSelectedListener(this);

        friendAdapter = new adapterFriendArray(inflater.getContext(),persistentFriends.getFriends());
        friendName.setAdapter(friendAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "fragmentMap - onActivityCreated"); }
        myMapView.onCreate(savedInstanceState);
        myMapView.getMapAsync(this);
        //myMap = myMapView.getMap();
    }

    //
    //
    // Create and destroy the friend reference objecto together with the fragment
    //
    //
    @Override
    public void onPause() {
        super.onPause();
        friendData.close();
        friendData = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ( friendData == null ) {
            friendData = new persistentFriends(this);
        }
        friendData.registerCallback(this);
    }

    //
    //
    // Get Google map is initialized
    //
    //
    public void onMapReady (GoogleMap googleMap) {
        Log.v(LOGTAG,"**************************");
        Log.v(LOGTAG," MAP IS READY!!!!!!!!!!!!");
        Log.v(LOGTAG,"**************************");
        myMap = googleMap;
    }

    //
    //
    // Callbacks from actions from MAP
    //
    //
    public   void onCameraChange (CameraPosition position) {
        centerOnFriend = false;
    }
    public boolean onMarkerClick (Marker marker) {
        if ( !centerOnFriend ) {
            centerOnFriend = true;
        }
        if ( myMap != null && activeFriend != null && activeFriend.hasLocation() ) {
            try {
                myMap.moveCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
            } catch (IllegalStateException e) {
                // We could not change the camera of the Map
                if ( MainActivity.Debug > 2 ) { Log.e(LOGTAG,"GoogleMap IllegalStateException " + e.getMessage()); }
            }
        }
        return true;
    }


    //
    //
    // Callbacks from friend updates
    //
    //
    public void refreshFriendInfo () {
        friendAdapter.notifyDataSetChanged();

        if ( activeFriend == null ) { return; }

        if ( friendMarker != null ) {
            friendMarker.setPosition(activeFriend.getLatLng());
            friendMarker.setTitle(activeFriend.getShowText());
            friendMarker.setSnippet(activeFriend.getSecondaryText());
        }
        if ( friendCircle != null ) {
            friendCircle.setCenter(activeFriend.getLatLng());
            friendCircle.setRadius(activeFriend.getAccuracy());
        }

        if ( centerOnFriend && myMap != null && activeFriend != null && activeFriend.hasLocation() ) {
            try {
                //myMap.moveCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(activeFriend.getLatLng(), 10));
                //if ( myMap.get)

                //myMap.moveCamera(CameraUpdateFactory.newLatLngBounds(activeFriend.getLatLng(), 10));
                //myMap.moveCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
                //CameraUpdateFactory.newLatLng()
            } catch (IllegalStateException e) {
                // We could not change the camera of the Map
                if ( MainActivity.Debug > 2 ) { Log.e(LOGTAG,"GoogleMap IllegalStateException " + e.getMessage()); }
            }
        }
    }

    public void refreshFriendLocation () { }
    public void refreshFriends () { }

    //
    //
    // Callbacks for dropdown list
    //
    //
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)

        centerOnFriend = true;

        //activeFriend = (miataruFriend)parent.getItemAtPosition(pos);

        activeFriend = persistentFriends.getFriends().get(pos);

        Log.v(LOGTAG,"Friend selected from spinner: " + activeFriend.getShowText());

        if ( myMap != null && activeFriend != null && activeFriend.hasLocation() ) {
            Log.v(LOGTAG,"*** Creating marker and circle");
            if (friendMarker == null) {
                friendMarker = myMap.addMarker(activeFriend.getMarkerOptions());
                friendMarker.setDraggable(false);
            } else {
                friendMarker.setPosition(activeFriend.getLatLng());
                friendMarker.setTitle(activeFriend.getShowText());
                friendMarker.setSnippet(activeFriend.getSecondaryText());

                friendMarker.setVisible(true);
            }
            if ( friendCircle == null ) {
                friendCircle = myMap.addCircle(activeFriend.getCircleOptions());
            } else {
                friendCircle.setCenter(activeFriend.getLatLng());
                friendCircle.setRadius(activeFriend.getAccuracy());

                friendCircle.setVisible(true);
            }

            // Center the camera in the selected friend
            Log.v(LOGTAG,"*** Zooming the camera");
            try {
                //myMap.moveCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(activeFriend.getLatLng(),10));
                //if ( myMap.get)

                //myMap.moveCamera(CameraUpdateFactory.newLatLngBounds(activeFriend.getLatLng(), 10));
                //myMap.moveCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
                //CameraUpdateFactory.newLatLng()
            } catch (IllegalStateException e) {
                // We could not change the camera of the Map
                if (MainActivity.Debug > 2) {
                    Log.e(LOGTAG, "GoogleMap IllegalStateException " + e.getMessage());
                }
            }
        }
    }
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        activeFriend = null;
        centerOnFriend = false;
        if ( friendMarker != null ) {
            friendMarker.setVisible(false);
        }
        if ( friendCircle != null ) {
            friendCircle.setVisible(false);
        }

    }

    //
    //
    // Adapter for friend spinner (drop down list)
    //
    //
    public class adapterFriendArray extends ArrayAdapter<miataruFriend> {
        private final Context context;
        private final ArrayList<miataruFriend> values;

        public adapterFriendArray(Context context, ArrayList<miataruFriend> values) {
            super(context, R.layout.friend_row_layout, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textDisplay = (TextView) convertView;

            if (textDisplay == null) {
                textDisplay = new TextView(context);
            }

            textDisplay.setText(values.get(position).getShowText());
            textDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            return textDisplay;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            View rowView = convertView;

            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.friend_row_layout, parent, false);
            }

            TextView update = (TextView) rowView.findViewById(R.id.lastupdate);
            TextView maintext = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondarytext = (TextView) rowView.findViewById(R.id.secondLine);

            miataruFriend friend = values.get(position);

            maintext.setText(friend.getShowText());
            secondarytext.setText(friend.getSecondaryText());

            update.setText(friend.getUpdateTime());

            return rowView;
        }
    }
}
