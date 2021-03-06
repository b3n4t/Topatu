package com.example.topatu;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

/**
 * Created by Ixa on 18/02/2015.
 */
public class fragmentMap extends Fragment implements persistentFriends.friendEvents, AdapterView.OnItemSelectedListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener {

    private static String LOGTAG = "TopatuLog";
    private static final int DEFAULT_ZOOM = 12;
    public static final String SAVESTATE_SPINNER_SELECTION = "fragmentMap-friendSpinner";

    private persistentFriends friendData = null;
    private boolean centerOnFriend = false;
    private boolean cameraTriggered = false;
    private FrameLayout frame = null;
    private MapView myMapView = null;
    private GoogleMap myMap = null;
    private Marker friendMarker = null;
    private TextView markerSpinner = null;
    private TextView friendInfo = null;
    private Circle friendCircle = null;
    private miataruFriend activeFriend = null;
    private long activeFriendTimeStamp = 0;
    private Spinner friendName = null;
    private Spinner chooseFriend;
    private int chosenFriend = -1;
    private adapterFriendArray friendAdapter;
    private Menu myMenu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "fragmentMap - onCreateView"); }

        setHasOptionsMenu(true);

        Bundle myParams;
        if ( savedInstanceState != null ) {
            //if ( MainActivity.Debug > 2 ) {  Log.v(LOGTAG,"***** reading parameters from savedInstanceState" ); }
            myParams = savedInstanceState;
        } else {
            myParams = getArguments();
            //if ( MainActivity.Debug > 2 ) {  Log.v(LOGTAG,"***** reading parameters from getArguments()" ); }
        }

        //View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        //textView.setText("Map goes here");
        //return rootView;

        //frame = (FrameLayout)inflater.inflate(R.layout.fragment_map, container, false);
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //MapsInitializer.initialize(inflater.getContext());

        friendInfo = (TextView)view.findViewById(R.id.map_friend_data);

        myMapView = (MapView)view.findViewById(R.id.map_googlemap);
        myMapView.onCreate(myParams);

        //myMapView.getMapAsync(this);
        myMap = myMapView.getMap();

        myMap.setOnCameraChangeListener(this);
        myMap.setOnMarkerClickListener(this);
        //myMap.setInfoWindowAdapter(new multiLineSpinner());


        myMap.setMyLocationEnabled(false);
        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.getUiSettings().setMyLocationButtonEnabled(false);
        myMap.getUiSettings().setZoomControlsEnabled(false);
        myMap.getUiSettings().setRotateGesturesEnabled(false);
        myMap.getUiSettings().setTiltGesturesEnabled(false);

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());


        // configured friend dropdown-list
        friendName = (Spinner)view.findViewById(R.id.map_friend_show);
        // set adapter to spinner
        friendAdapter = new adapterFriendArray(inflater.getContext(),persistentFriends.getFriends());
        //friendName.setAdapter(friendAdapter);

        if ( myParams != null ) {
            if (myParams.containsKey(SAVESTATE_SPINNER_SELECTION)) {
                chosenFriend = myParams.getInt(SAVESTATE_SPINNER_SELECTION);
                friendName.setSelection(myParams.getInt(SAVESTATE_SPINNER_SELECTION));
                if (MainActivity.Debug > 10) {
                    Log.v(LOGTAG, "fragmentMap - Restoring selected friend " + chosenFriend + " from list of " + persistentFriends.getFriends().size());
                }
                if (MainActivity.Debug > 10) {
                    Log.v(LOGTAG, "***** Restoring selected friend num " + chosenFriend);
                }
            } else {
                if ( MainActivity.Debug > 10 ) {  Log.v(LOGTAG,"***** Could not restore selected friend num, SAVESTATE_SPINNER_SELECTION doesn't exist" ); }
            }
        } else {
            if ( MainActivity.Debug > 10 ) {  Log.v(LOGTAG, "***** Could not restore selected friend num, myParams is empty"); }
        }
        friendName.setOnItemSelectedListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        if ( myMapView != null ) { myMapView.onSaveInstanceState(outState); }
        if ( friendName != null ) {
            //outState.putInt(SAVESTATE_SPINNER_SELECTION,friendName.getSelectedItemPosition());
            outState.putInt(SAVESTATE_SPINNER_SELECTION,chooseFriend.getSelectedItemPosition());
            if ( MainActivity.Debug > 10 ) {  Log.v(LOGTAG,"***** Saving selected friend num " + friendName.getSelectedItemPosition() ); }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_map_view, menu);
        myMenu = menu;

        MenuItem menuItem= menu.findItem(R.id.choose_friend);
        chooseFriend = (Spinner) MenuItemCompat.getActionView(menuItem);
        chooseFriend.setAdapter(friendAdapter);
        chooseFriend.setOnItemSelectedListener(this);
        if ( chosenFriend != -1 ) {
            chooseFriend.setSelection(chosenFriend);
        }


        Log.v(LOGTAG,"fragmentMap - onCreateOptionsMenu");
    }

    //
    //
    // Create and destroy the friend reference objects together with the fragment
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
        myMapView.onResume();

        if ( friendData == null ) {
            friendData = new persistentFriends(this);
        }
        friendData.registerCallback(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( myMapView != null ) { myMapView.onDestroy(); }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if ( myMapView != null ) { myMapView.onLowMemory(); }
    }

    //
    //
    // Callbacks from actions from MAP
    //
    //
    public   void onCameraChange (CameraPosition position) {
        //Log.v(LOGTAG,"**** onCameraChange " + cameraTriggered);
        centerOnFriend  = cameraTriggered;
        cameraTriggered = false;
    }
    public boolean onMarkerClick (Marker marker) {

        if ( centerOnFriend ) { centerOnFriend = true;cameraTriggered = true;return false; }

        if ( MainActivity.Debug > 0 ) { Log.e(LOGTAG,"fragmentMap - onMarkerClick"); }
        centerOnFriend = true;

        if ( myMap != null && marker != null ) {
            try {
                cameraTriggered = true;
                myMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
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
    public void onRefreshFriendInfo() {
        friendAdapter.notifyDataSetChanged();

        if ( activeFriend == null || !activeFriend.hasLocation() ) { return; }

        if ( activeFriendTimeStamp > 0 && activeFriendTimeStamp >= activeFriend.getTimeStamp() ) { return; }

        activeFriendTimeStamp = activeFriend.getTimeStamp();

        if ( friendInfo != null ) {
            friendInfo.setText(activeFriend.getLongDescription());
        }
        if ( friendMarker != null ) {
            friendMarker.setPosition(activeFriend.getLatLng());
            friendMarker.setTitle(activeFriend.getShowText());
            //friendMarker.setSnippet(activeFriend.getLongDescription());
        }
        if ( friendCircle != null ) {
            friendCircle.setCenter(activeFriend.getLatLng());
            friendCircle.setRadius(activeFriend.getAccuracy());
        }
        if ( markerSpinner != null ) {
            markerSpinner.setText(activeFriend.getLongDescription());
        }

        if ( centerOnFriend && myMap != null && activeFriend != null && activeFriend.hasLocation() ) {
            try {
                cameraTriggered = true;
                myMap.animateCamera(CameraUpdateFactory.newLatLng(activeFriend.getLatLng()));
            } catch (IllegalStateException e) {
                // We could not change the camera of the Map
                if ( MainActivity.Debug > 2 ) { Log.e(LOGTAG,"GoogleMap IllegalStateException " + e.getMessage()); }
            }
        }
    }

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

        if ( activeFriend != null && activeFriend.hasLocation() ) {
            activeFriendTimeStamp = activeFriend.getTimeStamp();
        } else {
            activeFriendTimeStamp = 0;
        }


        if ( activeFriend != null ) {
            if ( friendInfo != null ) {
                friendInfo.setText(activeFriend.getLongDescription());

            }

            Log.v(LOGTAG, "Friend selected from spinner: " + activeFriend.getShowText());

            if ( myMap != null && activeFriend.hasLocation() ) {
                //Log.v(LOGTAG, "*** Creating marker and circle");
                if (friendMarker == null) {
                        friendMarker = myMap.addMarker(activeFriend.getMarkerOptions());
                        friendMarker.setDraggable(false);
                    }
                    if (friendMarker != null) {
                        friendMarker.setVisible(false);
                        friendMarker.setPosition(activeFriend.getLatLng());
                        friendMarker.setTitle(activeFriend.getShowText());
                        //friendMarker.setSnippet(activeFriend.getShowText());
                        friendMarker.setVisible(true);
                    }
                    if (friendCircle == null) {
                        friendCircle = myMap.addCircle(activeFriend.getCircleOptions());
                    }
                    if (friendCircle != null) {
                        friendCircle.setVisible(false);
                        friendCircle.setCenter(activeFriend.getLatLng());
                        friendCircle.setRadius(activeFriend.getAccuracy());
                        friendCircle.setVisible(true);
                    }

                    // Center the camera in the selected friend
                    //Log.v(LOGTAG, "*** Zooming the camera");
                    try {
                        cameraTriggered = true;
                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(activeFriend.getLatLng(), DEFAULT_ZOOM));
                    } catch (IllegalStateException e) {
                        // We could not change the camera of the Map
                        if (MainActivity.Debug > 2) {
                            Log.e(LOGTAG, "GoogleMap IllegalStateException " + e.getMessage());
                        }
                    }
                } else {
                if (friendMarker != null) {
                    friendMarker.setVisible(false);
                }
                if (friendCircle != null) {
                    friendCircle.setVisible(false);
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
                //textDisplay.setTextColor(R.color.topatu_textcolor);
                //textDisplay.setBackgroundResource(R.color.topatu_backgroundtransparent);
            } else {
                Log.v(LOGTAG,"Recycling View from " + textDisplay.getText());
            }

            textDisplay.setTextColor(getResources().getColor(R.color.topatu_textcolor));
            textDisplay.setBackgroundColor(Color.TRANSPARENT);
            textDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            textDisplay.setPadding(3,3,3,3);
            textDisplay.setText(values.get(position).getShowText());

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

    //
    //
    // Adapter to be able to show tooltips with several lines in googleMap
    //
    //
    private class multiLineSpinner implements GoogleMap.InfoWindowAdapter {
        private final TextView mymarkerview;

        multiLineSpinner() {
            mymarkerview = new TextView(MainActivity.getAppContext());
        }

        public View getInfoWindow(Marker marker) {
            /*
            markerSpinner = mymarkerview;
            mymarkerview.setBackgroundResource(R.drawable.layout_rounded_bg);
            mymarkerview.setTextColor(getResources().getColor(R.color.topatu_textcolor));
            mymarkerview.setText(marker.getSnippet());

            return mymarkerview;
            */
            return null;
        }

        public View getInfoContents(Marker marker) {
            /*
            TextView v = new TextView(getActivity());
            v.setText(marker.getSnippet());
            return v;
            */
            return null;
        }

    }
}
