package com.example.topatu;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;
import java.util.UUID;

//

/**
 * Created by Ixa on 20/03/2015.
 */
public class MainActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {

    private static final String LOGTAG = "TopatuLog";
    private static final String STATE_ACTIVE_FRAGMENT = "Topatu_MainActivity_ActiveFragment";
    private static final String STATE_NUM_FRAGMENTS = "Topatu_MainActivity_NumFragment";
    private static final String STATE_FRAGMENT_STATE = "Topatu_MainActivity_FragmentState";
    private static String[] fragment_titles;

    public static Context context;
    public static int Debug = 3;

    private int prevFragmentNum = -1;
    private Fragment prevFragment = null;
    private ArrayList<Bundle> fragSavedStates = new ArrayList<>();
    private Bundle creationState;

    private Intent uploadService;
    private persistentFriends friends;
    private topatuConfig myConfig;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creationState = savedInstanceState;

        //
        // Check for UUID
        //
        myConfig = new topatuConfig(this);

        String MyID = myConfig.getID();

        if (MyID == null) {
            if (MyID == null) {
                if (MainActivity.Debug > 2) {
                    Log.v(LOGTAG, "No UUID found");
                }
                MyID = UUID.randomUUID().toString();
                //MyID = "07b2a900-6b6b-4e38-9679-6f4610bbb076";
                if (MainActivity.Debug > 2) {
                    Log.v(LOGTAG, "New one: " + MyID);
                }
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString(getString(R.string.settings_my_id), MyID);
                //editor.putString(getString(R.string.settings_my_server), );
                editor.apply();
            }
        }

        //
        // Load the screen
        //
        MainActivity.context = getApplicationContext();

        setContentView(R.layout.fragment_placehonder);

        //ActionBar bar = getActionBar();
        ActionBar bar = getSupportActionBar();

        fragment_titles = getResources().getStringArray(R.array.fragments);

        //ArrayAdapter<String> actionbar_adapter = new ArrayAdapter<String>(bar.getThemedContext(),R.layout.actionbar_list_item,fragment_titles);
        SpinnerAdapter mySpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.fragments, android.R.layout.simple_spinner_dropdown_item);

        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(mySpinnerAdapter, this);


        //
        // Get a persistentFriend objecet instance
        //
        friends = new persistentFriends();

        if (savedInstanceState != null) {
            persistentFriends.onRestoreInstanceState(savedInstanceState);

            int preFrag = savedInstanceState.getInt(STATE_ACTIVE_FRAGMENT,-1);
            if ( preFrag != -1 ) {
                bar.setSelectedNavigationItem(preFrag);
            }
            int num = savedInstanceState.getInt(STATE_NUM_FRAGMENTS,-1);
            if ( num > 0 ) {
                for (int x = 0;x < num; x++) {
                    fragSavedStates.add(x,savedInstanceState.getBundle(STATE_FRAGMENT_STATE+x));
                }
            }
        } else {
            bar.setSelectedNavigationItem(1);
        }

        //
        // Start the background service
        //
        uploadService = new Intent(context, serviceLocationUploader.class);
        uploadService.putExtra("StartedFrom","MainActivity");
        context.startService(uploadService);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    //
    //
    // save persistentFriends
    //
    //
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG,"MainActivity - onSaveInstanceState - saving friend information"); }
        //outState.putString("MyID", MyID);
        persistentFriends.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_FRAGMENT,prevFragmentNum);

        // Save the state of the fragments
        //
        // First save the state of the active fragment
        prevFragment.onSaveInstanceState(fragSavedStates.get(prevFragmentNum));
        // Now save all in the bundle
        outState.putInt(STATE_NUM_FRAGMENTS,fragSavedStates.size());
        for (int x = 0;x < fragSavedStates.size(); x++) {
            outState.putBundle(STATE_FRAGMENT_STATE+x,fragSavedStates.get(x));
        }
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        friends.close();
        friends = null;
    }

    @Override
    protected void onStop() {
        super.onStop();

        context.stopService(uploadService);
    }

    //
    //
    // fragment selector listener
    //
    //
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // Change the fragment

        Log.v(LOGTAG, "MainActivity - onNavigationItemSelected - " + itemId);

        // Check if this is not the first time we run this method
        if ( prevFragmentNum != -1 ) {
            Bundle fragState;

            // Read the bundle from the Array. If it doesn't exit create a new one and add to array
            try {
                fragState = fragSavedStates.get(prevFragmentNum);
            } catch ( IndexOutOfBoundsException e ) {
                fragState = new Bundle();
                for (int x = fragSavedStates.size(); x<prevFragmentNum ;x++ ) {
                    fragState = new Bundle();
                    fragSavedStates.add(x,fragState);
                }
            }

            // Set bundle to fragment
            prevFragment.onSaveInstanceState(fragState);
        }

        Fragment newFragment;

        // Choose which fragment we should show
        switch(itemPosition) {
            case 0: newFragment = new fragmentMap();break;
            case 1: newFragment = new fragmentFriendView();break;
            //case 2: newFragment = new fragmentTopatuSettings();break;
            default: newFragment = new fragmentTopatuSettings();break;
        }

        // if a state bundle exist add it to the fragment
        try {
            newFragment.setArguments(fragSavedStates.get(itemPosition));
        } catch ( IndexOutOfBoundsException e ) {
            if ( prevFragmentNum == -1 ) {
                newFragment.setArguments(creationState);
            }
        }

        // Replace the visible fragment
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_placeholder, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        // Save the current fragment and fragment number
        prevFragment = newFragment;
        prevFragmentNum = itemPosition;

        return true;
    }

    //
    //
    // Some common functions for the application
    //
    //
    public static Context getAppContext() {
        return MainActivity.context;
    }

    public static boolean isWifiActive () {
        ConnectivityManager connManager = (ConnectivityManager) MainActivity.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    public static boolean isPowerConnected () {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if ( intent == null ) { return false; }
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }
}