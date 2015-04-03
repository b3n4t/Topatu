package com.example.topatu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Tricky on 21/02/2015.
 */


public class persistentFriends extends BroadcastReceiver {
    private final static String LOGTAG = "TopatuLog";
    private static storageFriends dbHelper = null;
    private static SQLiteDatabase database = null;
    private static ArrayList<miataruFriend> friendList = new ArrayList<miataruFriend>();
    private static ArrayList<friendEvents> callbacks = new ArrayList<friendEvents>();
    private static ArrayList<AsyncTask> tasks = new ArrayList<AsyncTask>();
    private static ArrayList<Object> instances = new ArrayList<Object>();
    private static String myID;
    private static String myServer;
    private static miataruFriend mySelf;
    private static miataruFriend mySelfInCloud;

    private static LocationManager locationManager;
    private static LocationListener locationListener;

    private static final String SAVESTATE_NUM = "friendStateCount";
    private static final String SAVESTATE_FRIENDS = "friendStateFriendList";

    private static AlarmManager alarmMgr;
    private static PendingIntent alarmIntent;
    private static BroadcastReceiver friendLocationReceiver;

    private topatuConfig config;
    private friendEvents myCallback;
    private boolean clean = true;

    public persistentFriends() {
        this.defaultConfig();
    }

    public persistentFriends(friendEvents callback) {
        this.defaultConfig();
        this.registerCallback(callback);
    }

    private void defaultConfig() {
        myCallback = null;
        config = new topatuConfig(MainActivity.getAppContext());

        if (MainActivity.Debug > 3) {
            Log.v(LOGTAG, "persistentFriends - new ");
        }
        if (MainActivity.Debug > 0) {
            Log.v(LOGTAG, "persistentFriends - " + (instances.size() + 1) + " until now");
        }

        // If we are the first ones, open the DB
        if (instances.size() == 0 && dbHelper != null) {
            Log.e(LOGTAG, "First Friends object, but DB is already configured!");
            //throw new Exception("Internal error - Fit Friends object, but DB is already configured");
            return;
        }

        if ( dbHelper == null ) {
            dbHelper = new storageFriends(MainActivity.getAppContext());
        }

        if (instances.size() == 0) {
            //DBOpen();

            if (friendList.size() == 0 || friendList.get(0).equals(mySelf)) {
                friendList.addAll(fillDefault());
                new loadFromDB().execute();
            }
        }

        instances.add(this);
        clean = false;
    }

    private ArrayList<miataruFriend> fillDefault () {
        ArrayList<miataruFriend> f = new ArrayList<miataruFriend>();

        if ( myID == null ||myServer == null ) {
            //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext());
            //myID = settings.getString(MainActivity.getAppContext().getString(R.string.settings_my_id), null);
            //myServer = settings.getString(MainActivity.getAppContext().getString(R.string.settings_my_server), MainActivity.getAppContext().getString(R.string.settings_my_server_default));
            myID = config.getID();
            myServer = config.getServer();
        }
        if ( myID != null && mySelf == null) {
            mySelf = new miataruFriend(myID, MainActivity.getAppContext().getString(R.string.settings_my_server_local), MainActivity.getAppContext().getString(R.string.showtext_myid_local));
            f.add(mySelf);
            if (MainActivity.Debug > 0) {
                mySelfInCloud = new miataruFriend(myID, myServer, MainActivity.getAppContext().getString(R.string.showtext_myid_remote));
                f.add(mySelfInCloud);
            }
            if (MainActivity.Debug > 5) { Log.v(LOGTAG,"Adding myself to the list " + mySelf.getUUID()); }
        }
        return f;
    }


    protected void finalize() {
        if (!clean) {
            this.close();
        }
    }

    public void close() {
        if (MainActivity.Debug > 3) {
            Log.v(LOGTAG, "Instance closed. Current status: " + instances.size() + " instances, " + callbacks.size() + " callbacks");
        }
        if (myCallback != null) {
            callbacks.remove(myCallback);
            myCallback = null;

            if (callbacks.size() == 0) {
                // This was the last one waiting for any feedback
                // Stop any pending HTTP async tasks
                disableFriendPullJob();
            }

            locationManager.removeUpdates(locationListener);
        }

        //activeinstances--;
        instances.remove(this);
        clean = true;
    }

    public void registerCallback(friendEvents callback) {
        if (myCallback != null) {
            callbacks.remove(myCallback);
            myCallback = callback;
            callbacks.add(myCallback);
        } else {
            myCallback = callback;
            callbacks.add(myCallback);

            if (callbacks.size() == 1) {
                // This is the firs one interested in life data.
                // Start HTTP async tasks
                enableFriendPullJob();
            }
        }
    }

    public static void onSaveInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.putInt(SAVESTATE_NUM, friendList.size());
            if (friendList.size() > 0) {
                savedInstanceState.putParcelableArrayList(SAVESTATE_FRIENDS, friendList);
            }
        }
    }

    public static void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int numFriends = savedInstanceState.getInt(SAVESTATE_NUM, 0);
            if (MainActivity.Debug > 3) {
                Log.v(LOGTAG, "persistentFriends - onRestoreInstanceState: " + numFriends + " friends in save bundle");
            }
            if (numFriends > 0 && savedInstanceState.containsKey(SAVESTATE_FRIENDS)) {
                if (MainActivity.Debug > 3) {
                    Log.v(LOGTAG, "persistentFriends - onRestoreInstanceState: " + friendList.size() + " friends original list");
                }
                //friendList.clear();
                ArrayList<miataruFriend> friendsFromSave = savedInstanceState.getParcelableArrayList(SAVESTATE_FRIENDS);
                if (MainActivity.Debug > 3) {
                    Log.v(LOGTAG, "persistentFriends - onRestoreInstanceState: " + friendsFromSave.size() + " objects read");
                }

                mergeLists(friendList, friendsFromSave);
                friendsFromSave = null;
            } else {
                if (MainActivity.Debug > 3) {
                    Log.v(LOGTAG, "persistentFriends - onRestoreInstanceState: could not find any object");
                }
            }
        }

    }

    private void enableFriendPullJob() {
        if (MainActivity.Debug > 2) {
            Log.v(LOGTAG, "persistentFriends - enableFriendPullJob");
        }
        //
        //
        // Get my own location
        if ( locationManager  == null ) {
            locationManager = (LocationManager) MainActivity.getAppContext().getSystemService(Context.LOCATION_SERVICE);
        }

        // Get current location
        if (mySelf != null) {
            // Get the location manager
            //LocationManager locationManager = (LocationManager) MainActivity.getAppContext().getSystemService(Context.LOCATION_SERVICE);
            // Define the criteria how to select the location provider -> use default
            //Criteria criteria = new Criteria();
            //String provider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));
            if (location != null) {
                mySelf.setLocation(location);
            } else {
                Log.v(LOGTAG,"LOCATION IS NULLLLLLLLL");
            }
        }

        if ( locationListener == null ) {
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) { myLocationChanged(location); }
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            };
        }

        // Activate the location listeners
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, locationListener);

        //
        // Pull friend location
        //
        int interval = 60;
        // If wifi or power is available set a fast refresh
        // else try to not use a lot of battery/bandwidth
        if ( MainActivity.isWifiActive() ) {
            if ( MainActivity.isPowerConnected() ) {
                interval = 6;
            } else {
                interval = 15;
            }
        } else {
            interval = 60;
        }

        if ( MainActivity.Debug > 0 ) {
            interval = 5;
        }

        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) MainActivity.getAppContext().getSystemService(Context.ALARM_SERVICE);
        }
        if (alarmIntent == null) {
            Intent intent = new Intent(MainActivity.getAppContext(), receiverPullFriendData.class);
            alarmIntent = PendingIntent.getBroadcast(MainActivity.getAppContext(), 0, intent, 0);
        }

        Calendar cal = Calendar.getInstance();

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, interval * 1000, alarmIntent);
        //alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
        // SystemClock.elapsedRealtime() +
        // interval * 1000, alarmIntent);

        //
        // Now register the receiver to get the answers
        //
        if ( friendLocationReceiver == null ) {
            friendLocationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean changes = false;

                    if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "persistentFriends - getFeedback - onReceive"); }

                    // Callback from friend location update
                    Bundle b = intent.getExtras();

                    if (b != null) {
                        ArrayList<miataruFriend> friendsLocation = b.getParcelableArrayList("FriendInfo");

                        if (friendsLocation != null && friendsLocation.size() > 0) {
                            changes = mergeLists(friendList, friendsLocation);
                        }
                    }

                    if (changes) {
                        // do all the callbacks
                        callingTheCallbacks();
                    }

                }
            };
        }

        IntentFilter filter = new IntentFilter("com.example.topatu.friendlocationinformation");
        MainActivity.getAppContext().registerReceiver(friendLocationReceiver, filter);
    }

    private void disableFriendPullJob() {
        if (MainActivity.Debug > 2) {
            Log.v(LOGTAG, "persistentFriends - disableFriendPullJob");
        }
        alarmMgr.cancel(alarmIntent);
        MainActivity.getAppContext().unregisterReceiver(friendLocationReceiver);
        //locationManager.requestLocationUpdates
        locationManager.removeUpdates(locationListener);
    }

    private void callingTheCallbacks () {
        for (int x = 0; x < callbacks.size(); x++) {
            callbacks.get(x).onRefreshFriendInfo();
        }
    }

    //
    //
    // Location listener object
    //
    //
    private void myLocationChanged (Location location) {
        Log.v(LOGTAG, "Getting new location from " + location.getProvider());
        if ( mySelf != null ) {
            // TODO We need to processing here, but meanwhile....

            if ( mySelfInCloud != null ) {
                mySelfInCloud.setServer(config.getServer());
            }

            mySelf.setLocation(location);
            callingTheCallbacks();
        }
    }

    //
    //
    // General methods to add remove friends from DB
    //
    //
    private miataruFriend friendExists(String UUID, String server ) {
        if ( UUID != null && server != null ) {
            miataruFriend friend;
            for (int x = 0; x < friendList.size(); x++) {
                friend = friendList.get(x);
                if (friend.getUUID().compareTo(UUID) == 0 && friend.getServer().compareTo(server) == 0) {
                    return friend;
                }
            }
        }
        return null;
    }

    public static String getMyID () {
        return myID;
    }

    public static miataruFriend getMySelf () {
        return mySelf;
    }
    private boolean checkIfMyself ( miataruFriend friend ) {
        if ( mySelf == null ) {
            return false;
        }
        if ( friend.equals(mySelf) ) {
            return true;
        }
        if ( friend.equals(mySelfInCloud) ) {
            return true;
        }
        return false;
    }

    public void addFriend(miataruFriend newFriend) {
        if ( newFriend != null ) {
            if ( friendList.indexOf(newFriend) >= 0 || friendExists(newFriend.getUUID(),newFriend.getServer()) != null ) {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_exists, Toast.LENGTH_LONG).show();
            } else {
                this.addFriend(newFriend.getUUID(),newFriend.getServer(),newFriend.getAlias());
            }
        }
    }

    public void addFriend(String UUID, String Server) {
        this.addFriend(UUID, Server, null);
    }

    public void addFriend(String UUID, String server, String alias) {
        // Check if UUID has some content
        if (UUID == null || UUID.length() == 0 || server == null || server.length() == 0) {
            Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_invalid_friend_data, Toast.LENGTH_LONG).show();
            return;
        }
        // Check that we don't have already that UUID
        for (int x = 0; x < friendList.size(); x++) {
            if (friendList.get(x).getUUID().compareTo(UUID) == 0 && friendList.get(x).getServer().compareTo(server) == 0) {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_exists, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Add new friend to the list
        miataruFriend newFriend = new miataruFriend(UUID, server, alias);
        friendList.add(newFriend);

        callingTheCallbacks();

        // Add the new object the DB
        //this.DBAddFriend(newFriend);
        new addToDB().execute(newFriend);
    }

    public void removeFriend(miataruFriend friend) {
        if ( !checkIfMyself(friend) ) {
            if (friendList.contains(friend)) {
                friendList.remove(friend);
                callingTheCallbacks();
                new deleteFromDB().execute(friend);
            }
        }
    }

    public void removeFriend(String UUID, String server) {
        boolean found = false;
        miataruFriend friend = null;

        if ( UUID != null && server != null ) {
            for (int x = 0; x < friendList.size(); x++) {
                friend = friendList.get(x);
                if (friend.getUUID().compareTo(UUID) == 0 && friend.getServer().compareTo(server) == 0) {
                    found = true;
                    break;
                }
            }

            if (found && friend != null && ! checkIfMyself(friend)) {
                friendList.remove(friend);
                callingTheCallbacks();
                new deleteFromDB().execute(friend);
            }
        }
    }

    public void editFriend(miataruFriend current, String UUID, String server, String alias) {
        String newID = UUID;
        String newServer = server;
        String newAlias = alias;
        boolean newData = false;
        Bundle changes = new Bundle();

        if ( UUID != null || server != null || alias != null ) {
            if ( newID != null &&  newID.compareTo(current.getUUID()) != 0 ) {
                newData = true;
                changes.putString("OrigUUID", current.getUUID());
            } else {
                newID = current.getUUID();
            }
            if ( newServer != null && newServer.compareTo(current.getServer()) != 0 ) {
                newData = true;
                changes.putString("OrigServer", current.getServer());
            } else {
                newServer = current.getServer();
            }
            if ( newData && friendExists(newID,newServer) != null ) {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_exists, Toast.LENGTH_LONG).show();
                return;
            }

            if ( newAlias != null && newAlias.compareTo(current.getAlias()) != 0 ) {
                newData = true;
                changes.putString("OrigAlias", current.getAlias() );
            }
            if ( newData ) {
                current.setUUID(UUID);
                current.setServer(server);
                current.setAlias(alias);


                current.putExtras(changes);
                callingTheCallbacks();

                new editInDB().execute(current);
            } else {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_nothing_to_change, Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static ArrayList<miataruFriend> getFriends() {
        return friendList;
    }


    //
    //
    // Helper function to transfer the friends from one list to the other
    //
    //
    private static boolean mergeLists(ArrayList<miataruFriend> oldFriends, ArrayList<miataruFriend> newFriends) {
        return mergeLists(oldFriends, newFriends, true);
    }

    private static boolean mergeLists(ArrayList<miataruFriend> oldFriends, ArrayList<miataruFriend> newFriends, boolean addOnly) {
        if (newFriends == null || oldFriends == null) {
            return false;
        }

        boolean dataChanged = false;

        int oldFriendCount = oldFriends.size();
        String[] oldUUIDs = new String[oldFriendCount];
        String[] oldServers = new String[oldFriendCount];
        String[] newUUIDs = new String[newFriends.size()];
        String[] newServers = new String[newFriends.size()];

        for (int x = 0; x < oldFriends.size(); x++) {
            oldUUIDs[x] = oldFriends.get(x).getUUID();
            oldServers[x] = oldFriends.get(x).getServer();
            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "mergeLists - old friend: " + oldUUIDs[x]);
            }
        }
        for (int x = 0; x < newFriends.size(); x++) {
            miataruFriend friend = newFriends.get(x);
            String ID = friend.getUUID();
            String server = friend.getServer();

            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "mergeLists - new friend: " + ID);
            }

            newUUIDs[x] = ID;
            newServers[x] = server;

            boolean newID = true;
            for (int n = 0; n < oldFriendCount; n++) {
                if ( ID.compareTo(oldUUIDs[n]) == 0 && server.compareTo(oldServers[n]) == 0 ) {
                    // Same UUID and server found. Copy info in case it has changed.
                    newID = false;
                    if (friend.getAlias() != null && friend.getAlias().length() > 0 && friend.getAlias() != null && friend.getAlias().compareTo(oldFriends.get(n).getAlias()) != 0) {
                        oldFriends.get(n).setAlias(friend.getAlias());
                        dataChanged = true;
                    }
                    if (friend.hasLocation()) {
                        if ( oldFriends.get(n).hasLocation() ) {
                            if ( friend.getTimeStamp() > oldFriends.get(n).getTimeStamp() ) {
                                // We have a newer location
                                if (MainActivity.Debug > 10) { Log.v(LOGTAG, "mergeLists - friend exists. updating the location info"); }
                                oldFriends.get(n).setLocation(friend.getLocation());
                                dataChanged = true;
                            } else {
                                // We have the a location, but is not newer that the current one
                                // nothing to do then....
                            }
                        } else {
                            // We have a location and we didn't have any until now
                            if (MainActivity.Debug > 10) { Log.v(LOGTAG, "mergeLists - friend exists. adding location info"); }
                            oldFriends.get(n).setLocation(friend.getLocation());
                            dataChanged = true;
                        }
                    }
                    if (MainActivity.Debug > 5) {
                        Log.v(LOGTAG, "mergeLists updating " + friend.getShowText());
                    }
                    break;
                }
            }

            if (newID) {
                if (MainActivity.Debug > 5) {
                    Log.v(LOGTAG, "mergeLists adding " + friend.getShowText());
                }

                oldFriends.add(friend);
                dataChanged = true;
            }
        }

        // Check if some of the friends we have are gone in the new load and delete them
        // Should not happen, just for consistency
        if (!addOnly) {
            for (int x = oldFriendCount - 1; x >= 0; x--) {
                String ID = oldFriends.get(x).getUUID();
                boolean missing = true;

                for (int n = 0; n < newFriends.size(); n++) {
                    if (ID.compareTo(newUUIDs[n]) == 0) {
                        missing = false;
                        break;
                    }
                }

                if (missing) {
                    // Check we are not trying to remove our own
                    if ( oldFriends.get(x).getServer().compareTo(MainActivity.getAppContext().getString(R.string.settings_my_server_local)) != 0 ) {
                        if (MainActivity.Debug > 5) {
                            Log.v(LOGTAG, "mergeLists removing " + oldFriends.get(x).getShowText());
                        }
                        oldFriends.remove(x);
                        dataChanged = true;
                    }
                }
            }
        }

        return dataChanged;
    }

    //
    //
    // Asynchronous tasks for DB operations
    //
    //

    // Load full DB
    public class loadFromDB extends AsyncTask<Void, Void, ArrayList<miataruFriend>> {

        @Override
        protected void onPreExecute() {
            tasks.add(this);
        }

        @Override
        protected ArrayList<miataruFriend> doInBackground(Void... none) {
            // Do SQL select and read all the friend info
            ArrayList<miataruFriend> internalFriends = new ArrayList<miataruFriend>();
            //internalFriends.add(new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216", "service.miataru.com", "Demo Miataru device"));

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String[] columns = {
                    storageFriends.COLUMN_UUID,
                    storageFriends.COLUMN_SERVER,
                    storageFriends.COLUMN_ALIAS
            };

            Cursor cursor = db.query(
                    storageFriends.TABLE_FRIENDS,
                    columns,
                    null,    // selection
                    null,    // selectionArgs
                    null,    // groupBy
                    null,    // having
                    null);   // orderBy

            ArrayList<miataruFriend> friendsFromDB = new ArrayList<miataruFriend>();

            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                miataruFriend friend;

                String ID = cursor.getString(cursor.getColumnIndex(storageFriends.COLUMN_UUID));
                String Server = cursor.getString(cursor.getColumnIndex(storageFriends.COLUMN_SERVER));
                String Alias = cursor.getString(cursor.getColumnIndex(storageFriends.COLUMN_ALIAS));


                friend = new miataruFriend(ID, Server);
                if ( Alias != null ) {
                    friend.setAlias(Alias);
                }

                internalFriends.add(friend);

                cursor.moveToNext();
            }

            db.close();

            return internalFriends;

        }

        @Override
        protected void onPostExecute(ArrayList<miataruFriend> friendsFromDB) {
            if (MainActivity.Debug > 2) {
                Log.v(LOGTAG, "persistentFriends - Friends loaded from DB: " + friendsFromDB.size());
            }

            friendsFromDB.addAll(fillDefault());

            tasks.remove(this);
            boolean newData = false;
            // Load the friend list to internal variable
            if (friendList.size() > 0) {
                // There is some data already. compare and look for new
                // We plan to use this call only for initial load, so this is still empty
                if (MainActivity.Debug > 5) {
                    Log.v(LOGTAG, "persistentFriends - Merging " + friendsFromDB.size() + " to " + friendList.size());
                }

                if ( mySelf != null ) {
                    friendsFromDB.add(mySelf);
                }
                newData = mergeLists(friendList, friendsFromDB, false);
            } else {
                friendList.addAll(friendsFromDB);
                newData = true;
            }

            if (MainActivity.Debug > 5) { Log.v(LOGTAG, "persistentFriends - Friend list changed: " + newData); }

            // call all the callbacks
            if (newData) {
                callingTheCallbacks();
            }
        }
    }

    //
    //
    // AsyncTask to remove one friend from the list
    //
    //
    private class deleteFromDB extends AsyncTask<miataruFriend, Void, miataruFriend> {
        @Override
        protected void onPreExecute() {
            tasks.add(this);
        }

        @Override
        protected miataruFriend doInBackground(miataruFriend... friendToDelete) {
            // Delete friend from DB
            // If success return "null", if error return the friend to be added again to the list
            if (friendToDelete.length == 0) {
                return null;
            }

            miataruFriend friend = friendToDelete[0];

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String selection = storageFriends.COLUMN_UUID + " LIKE ? AND " + storageFriends.COLUMN_SERVER + " LIKE ?";
            String[] selectionArgs = { friend.getUUID(), friend.getServer() };

            int numRows = db.delete(
                    storageFriends.TABLE_FRIENDS,
                    selection,
                    selectionArgs
            );

            db.close();

            if ( numRows == 1 ) {
                return null;
            } else {
                return friend;
            }
        }

        @Override
        protected void onPostExecute(miataruFriend deletedFriend) {
            tasks.remove(this);
            if (deletedFriend == null) {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_deleted, Toast.LENGTH_SHORT).show();
                //friendList.remove(deletedFriend);
            } else {
                Toast.makeText(MainActivity.getAppContext(), R.string.showtext_alert_friend_couldnt_delete, Toast.LENGTH_SHORT).show();
                friendList.add(deletedFriend);
                callingTheCallbacks();
            }
        }
    }

    //
    //
    // AsyncTask to add one friend to the list
    //
    //
    private class addToDB extends AsyncTask<miataruFriend, Void, miataruFriend> {
        @Override
        protected void onPreExecute() {
            tasks.add(this);
        }

        @Override
        protected miataruFriend doInBackground(miataruFriend... friendToDelete) {
            // Delete friend from DB
            // If success return "null", if error return the friend to be added again to the list
            if (friendToDelete.length == 0) {
                return null;
            }

            miataruFriend friend = friendToDelete[0];

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues ();
            values.put(storageFriends.COLUMN_UUID, friend.getUUID());
            values.put(storageFriends.COLUMN_SERVER, friend.getServer());
            values.put(storageFriends.COLUMN_ALIAS, friend.getAlias());

            long newRowId = db.insert(storageFriends.TABLE_FRIENDS, null, values );

            if ( newRowId == -1 ) {
                return friend;
            } else {
                return null;
            }

        }

        @Override
        protected void onPostExecute(miataruFriend deletedFriend) {
            tasks.remove(this);
            if (deletedFriend == null) {
                Toast.makeText(MainActivity.getAppContext(), "Friend added", Toast.LENGTH_SHORT).show();
                //friendList.remove(deletedFriend);
            } else {
                Toast.makeText(MainActivity.getAppContext(), "Friend could not be added", Toast.LENGTH_SHORT).show();
                friendList.remove(deletedFriend);
                callingTheCallbacks();
            }
        }
    }

    //
    //
    // AsyncTask to modify a friend
    //
    //
    private class editInDB extends AsyncTask<miataruFriend, Void, miataruFriend> {
        @Override
        protected void onPreExecute() {
            tasks.add(this);
        }

        @Override
        protected miataruFriend doInBackground(miataruFriend... friendToChange) {
            // Edit friend in DB
            // If success return "null", if error return the friend to be rolledback
            boolean allOK = true;

            if (friendToChange.length == 0) {
                return null;
            }

            miataruFriend friend = friendToChange[0];

            Bundle originals = friend.getExtras();

            String origUUID = originals.getString("OrigUUID",null);
            if ( origUUID == null ) {
                origUUID = friend.getUUID();
            }

            String origServer = originals.getString("OrigServer",null);
            if ( origServer == null ) {
                origServer = friend.getServer();
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String selection = storageFriends.COLUMN_UUID + " LIKE ? AND " + storageFriends.COLUMN_SERVER + " LIKE ?";
            String[] selectionArgs = { origUUID, origServer };
            String[] columns = { storageFriends.COLUMN_UUID };

            Cursor c = db.query(
                    storageFriends.TABLE_FRIENDS,
                    columns,
                    selection,
                    selectionArgs,
                    null,    // groupBy
                    null,    // having
                    null);   // orderBy

            if ( c.getCount() == 1 ) {
                // All is OK, change the values

                ContentValues values = new ContentValues ();
                values.put(storageFriends.COLUMN_UUID, friend.getUUID());
                values.put(storageFriends.COLUMN_SERVER, friend.getServer());
                values.put(storageFriends.COLUMN_ALIAS, friend.getAlias());

                int count = db.update(
                        storageFriends.TABLE_FRIENDS,
                        values,
                        selection,
                        selectionArgs);


                if ( count == 0 ) {
                    // somehow we didn't change anything return the friend, we should do a full reread from DB
                    allOK = false;
                } else if ( count == 1 ) {
                    // all ok, return success
                    allOK = true;
                } else {
                    // How is it possible that we modified more than one line???
                    // we should create an exception
                    allOK = false;
                }
            } else {
                // we found something strange, quit without changes. return null, we should do a full reread from DB
                allOK = false;
            }

            if ( allOK ) {
                originals.remove("OrigUUID");
                originals.remove("OrigServer");
                originals.remove("OrigAlias");
                return null;
            } else {
                return friend;
            }
        }

        @Override
        protected void onPostExecute(miataruFriend changedFriend) {
            tasks.remove(this);
            if (changedFriend == null) {
                Toast.makeText(MainActivity.getAppContext(), "Friend changed", Toast.LENGTH_SHORT).show();
                //friendList.remove(deletedFriend);
            } else {
                Toast.makeText(MainActivity.getAppContext(), "Friend could not be changed", Toast.LENGTH_SHORT).show();
                Bundle oldData = changedFriend.getExtras();

                if ( oldData.getString("OrigUUID",null) != null ) {
                    changedFriend.setUUID(oldData.getString("OrigUUID"));
                    oldData.remove("OrigUUID");
                }
                if ( oldData.getString("OrigServer",null) != null ) {
                    changedFriend.setServer(oldData.getString("OrigServer"));
                    oldData.remove("OrigServer");
                }
                if ( oldData.getString("OrigAlias",null) != null ) {
                    changedFriend.setAlias(oldData.getString("OrigAlias"));
                    oldData.remove("OrigAlias");
                }

                callingTheCallbacks();
            }
        }
    }


    //
    //
    // Asynchronous tasks for Miataru operations
    //
    //
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean changes = false;

        if ( MainActivity.Debug > 5 ) { Log.v(LOGTAG, "persistentFriends - getFeedback - onReceive " + action); }

        if (action.compareTo("com.example.topatu.friendlocationinformation") == 0)

        {
            // Callback from friend location update
            Bundle b = intent.getExtras();

            if (b != null) {
                ArrayList<miataruFriend> friendsLocation = b.getParcelableArrayList("FriendInfo");

                if (friendsLocation != null && friendsLocation.size() > 0) {
                    mergeLists(friendList, friendsLocation);
                    changes = true;
                }
            }
        }

        if (changes) {
            // do all the callbacks
            callingTheCallbacks();
        }

        //this.close();
    }


    //
    //
    // Interface declaration for callbacks
    //
    //
     interface friendEvents {
        void onRefreshFriendInfo();
    }
}
