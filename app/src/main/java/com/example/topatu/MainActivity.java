package com.example.topatu;

import android.app.ActivityManager;
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
public class MainActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, fragmentFriendView.OnFriendSelected, fragmentTopatuSettings.OnConfigChanged {

    private static final String LOGTAG = "TopatuLog";
    private static final String STATE_ACTIVE_FRAGMENT = "Topatu_MainActivity_ActiveFragment";
    private static final String STATE_NUM_FRAGMENTS = "Topatu_MainActivity_NumFragment";
    private static final String STATE_FRAGMENT_STATE = "Topatu_MainActivity_FragmentState";
    private static final int FRAGMENT_MAP = 0;
    private static final int FRAGMENT_FRIENDS = 1;
    private static final int FRAGMENT_SETTINGS = 2;

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
        // Check for UUID and create a new one if missing. This should be changed in final release.
        //
        myConfig = new topatuConfig(this);

        String MyID = myConfig.getID();

        if (MyID == null) {
            if (MainActivity.Debug > 2) {
                Log.v(LOGTAG, "No UUID found");
            }
            MyID = UUID.randomUUID().toString();
            if (MainActivity.Debug > 2) {
                Log.v(LOGTAG, "New one: " + MyID);
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(getString(R.string.settings_my_id), MyID);
            editor.putString(getString(R.string.settings_my_server),getString(R.string.settings_my_server_default));
            editor.apply();
        }
        //
        // Load the screen
        //
        MainActivity.context = getApplicationContext();
        fragment_titles = getResources().getStringArray(R.array.fragments);

        setContentView(R.layout.fragment_placehonder);

        //ActionBar bar = getActionBar();
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        //ArrayAdapter<String> actionbar_adapter = new ArrayAdapter<String>(bar.getThemedContext(),R.layout.actionbar_list_item,fragment_titles);
        SpinnerAdapter mySpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.fragments, android.R.layout.simple_spinner_dropdown_item);
        bar.setListNavigationCallbacks(mySpinnerAdapter, this);

        //
        // Get a persistentFriend objecet instance
        //
        friends = new persistentFriends();


        // show default fragment, currently friendlist but probably map
        int default_fragment = FRAGMENT_FRIENDS;

        if (savedInstanceState != null) {
            // restore friend location if possible
            persistentFriends.onRestoreInstanceState(savedInstanceState);

            // Restore saved states from fragments
            int num = savedInstanceState.getInt(STATE_NUM_FRAGMENTS,-1);
            if ( num > 0 ) {
                for (int x = 0;x < num; x++) {
                    fragSavedStates.add(x,savedInstanceState.getBundle(STATE_FRAGMENT_STATE+x));
                }
            }

            // restore which fragment was active
            int preFrag = savedInstanceState.getInt(STATE_ACTIVE_FRAGMENT,-1);
            if ( preFrag != -1 ) {
                //Log.v(LOGTAG,"**** MainActivity - restoring previous fragment " + preFrag);
                default_fragment = preFrag;
            }
        } else {
            for (int x=0;x < fragment_titles.length;x++) {
                fragSavedStates.add(x,new Bundle());
            }
        }

        bar.setSelectedNavigationItem(default_fragment);

        //
        // Prepare Intent for background upload service
        //
        uploadService = new Intent(context, serviceLocationUploader.class);
        uploadService.putExtra("StartedFrom", "MainActivity");
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
    }

    //
    //
    // fragment selector listener
    //
    //
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // Change the fragment

        //Log.v(LOGTAG, "**** MainActivity - onNavigationItemSelected - " + itemPosition);

        // Check if this is not the first time we run this method
        if ( prevFragmentNum != -1 ) {
            Bundle fragState;

            // Read the bundle from the Array
            fragState = fragSavedStates.get(prevFragmentNum);

            // Set bundle to fragment
            prevFragment.onSaveInstanceState(fragState);
        }

        if ( prevFragmentNum != itemPosition ) {

            Fragment newFragment;

            // Choose which fragment we should show
            switch (itemPosition) {
                case FRAGMENT_MAP:
                    newFragment = new fragmentMap();
                    break;
                case FRAGMENT_FRIENDS:
                    newFragment = new fragmentFriendView();
                    break;
                //case 2: newFragment = new fragmentTopatuSettings();break;
                case FRAGMENT_SETTINGS:
                    newFragment = new fragmentTopatuSettings();
                    break;
                default:
                    newFragment = new fragmentTopatuSettings();
                    break;
            }

            // add state bundle to the fragment
            newFragment.setArguments(fragSavedStates.get(itemPosition));

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

        return false;
    }


    //
    //
    // Callbacks from fragments
    //
    //


    //@Override
    public void onFriendSelected(int friendPos) {
        // Add something here to pass the selected friend to the fragment

        // Call the fragment

        fragSavedStates.get(FRAGMENT_MAP).putInt(fragmentMap.SAVESTATE_SPINNER_SELECTION,friendPos);

        getSupportActionBar().setSelectedNavigationItem(FRAGMENT_MAP);

    }

    public void onConfigChanged () {
        topatuConfig config = new topatuConfig(this);

        if ( config.getID() != null && config.getID().length() > 0 ) {
            if (config.getUploadLocation() || config.getSaveHistory() ) {
                Log.v(LOGTAG,"*** Starting background service");
                context.startService(uploadService);
            } else {
                if (isMyServiceRunning(serviceLocationUploader.class)) {
                    Log.v(LOGTAG,"*** Stoping background service");
                    context.stopService(uploadService);
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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