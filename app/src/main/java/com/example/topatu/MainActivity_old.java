package com.example.topatu;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;
import java.util.UUID;

public class MainActivity_old extends ActionBarActivity implements ActionBar.TabListener, ActionBar.OnNavigationListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    private

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private static String LOGTAG = "TopatuLog";
    private String PREFS_NAME = null;
    private static String MyID;
    private static String MyServer;
    private persistentFriends friends;
    private int currentTab=0;

    private Intent uploadService;

    public static Context context;
    public static int Debug = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext();
        if ( MainActivity.Debug > 2 ) {
            Log.v(LOGTAG, "MainActivity - onCreate");
        }

        Log.v(LOGTAG,"*** Power: "+MainActivity.isPowerConnected());
        Log.v(LOGTAG,"*** Wifi: "+MainActivity.isWifiActive());

        if (savedInstanceState != null) {
            MyID = savedInstanceState.getString(getString(R.string.settings_my_id), null);
            MyServer = savedInstanceState.getString(getString(R.string.settings_my_server), getString(R.string.settings_my_server_default));
        }
        //
        // On first run there will be no Device ID generaten. Create a new one.
        //
        //PREFS_NAME  = getPreferenceManager().getSharedPreferencesName();
        if (MyID == null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            // my_id
            MyID = settings.getString(getString(R.string.settings_my_id), null);
            MyServer = settings.getString(getString(R.string.settings_my_server), getString(R.string.settings_my_server_default));
            if (MyID == null) {
                if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "No UUID found"); }
                MyID = UUID.randomUUID().toString();
                MyID = "07b2a900-6b6b-4e38-9679-6f4610bbb076";
                if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "New one: " + MyID); }
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(getString(R.string.settings_my_id), MyID);
                editor.apply();
            } else {
                if ( MainActivity.Debug > 0 ) { Log.v(LOGTAG, "Reading from config UUID: " + MyID); }
            }
        } else {
            if ( MainActivity.Debug > 0 ) { Log.v(LOGTAG, "App still running UUID: " + MyID); }

        }

        //
        // Create an instance of persistentFriends
        //
        friends = new persistentFriends();
        if (savedInstanceState != null) {
            if ( MainActivity.Debug > 2 ) {
                Log.v(LOGTAG, "MainActivity - trying to restore friendlist");
                Log.v(LOGTAG, "MainActivity -             current friends: " + persistentFriends.getFriends().size());
            }
            persistentFriends.onRestoreInstanceState(savedInstanceState);
            if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "MainActivity -             current friends: " + persistentFriends.getFriends().size()); }
        }


        //
        // Load main screen
        //
        setContentView(R.layout.activity_main);

        //
        // Set up the action bar.
        //
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                currentTab = position;
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        if (savedInstanceState != null) {
            // Go to previously selected tab
            int pos;
            pos = savedInstanceState.getInt("TopatuSelecetTab", -1);
            if (pos != -1) {
                mViewPager.setCurrentItem(pos);
                actionBar.setSelectedNavigationItem(pos);
            }
        }

        //
        // Start the background service
        //
        uploadService = new Intent(context, serviceLocationUploader.class);
        uploadService.putExtra("StartedFrom","MainActivity");
        context.startService(uploadService);
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
        outState.putString("MyID", MyID);
        outState.putInt("TopatuSelecetTab",currentTab);
        persistentFriends.onSaveInstanceState(outState);
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

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    //
    //
    // Callbacks for tab actions
    //
    //
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        currentTab = tab.getPosition();
        mViewPager.setCurrentItem(currentTab);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    //
    //
    // Callback for navigation spinner
    //
    //
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        currentTab = itemPosition;

        mViewPager.setCurrentItem(currentTab);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem(currentTab);

        return true;
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //if ( MainActivity.Debug > 2 ) { Log.v("Topatu", "SectionsPagerAdapter - getItem ("+position+")"); }
            Fragment f;
            switch(position) {
                //case 0: f = fragmentMap.newInstance();break;
                //case 1: f = fragmentFriendView.newInstance();break;
                //case 2: f = fragmentSettings.newInstance();break;
                default: f = PlaceholderFragment.newInstance(position + 1);break;
            }

            return f;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            String[] fragment_titles = getResources().getStringArray(R.array.fragments);
            return fragment_titles[position];
        }
    }



    /**
     * A placeholder fragment containing a simple view.
     */
    private static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "placeholderFragment - newInstance"); }
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

//        public PlaceholderFragment() {
//        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "placeholderFragment - onCreateView"); }
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textView.setText("Tab "+this.getArguments().getInt(ARG_SECTION_NUMBER,9));
            return rootView;
        }
/*
        @Override
        public void onPause() {
            super.onPause();
            if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "placeholderFragment - onPause"); }
        }

        @Override
        public void onResume() {
            super.onResume();
            if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "placeholderFragment - onResume"); }
        }
        */
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
