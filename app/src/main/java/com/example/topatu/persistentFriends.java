package com.example.topatu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Tricky on 21/02/2015.
 */


public class persistentFriends extends BroadcastReceiver {
    private final static String LOGTAG = "TopatuLog";
    private int activeinstances = 0;
    private static storageFriends dbHelper = null;
    private static SQLiteDatabase database = null;
    private static ArrayList<miataruFriend> friendList = new ArrayList<miataruFriend>();
    private static ArrayList<friendEvents> callbacks = new ArrayList<friendEvents>();
    private static ArrayList<AsyncTask> tasks = new ArrayList<AsyncTask>();
    private static ArrayList<Object> instances = new ArrayList<Object>();
    private friendEvents myCallback;
    private boolean clean = true;

    private static AlarmManager alarmMgr;
    private static PendingIntent alarmIntent;


    private static final String SAVESTATE_NUM = "friendStateCount";
    private static final String SAVESTATE_FRIENDS = "friendStateFriendList";

    public persistentFriends() {
        this.defaultConfig();
    }

    public persistentFriends(friendEvents callback) {
        this.defaultConfig();
        this.registerCallback(callback);
    }

    private void defaultConfig() {
        myCallback = null;

        if (MainActivity.Debug > 3) {
            Log.v(LOGTAG, "persistentFriends - new ");
        }
        if (MainActivity.Debug > 3) {
            Log.v(LOGTAG, "persistentFriends - " + (instances.size() + 1) + " until now");
        }

        // If we are the first ones, open the DB
        if (instances.size() == 0 && dbHelper != null) {
            Log.e(LOGTAG, "First Friends object, but DB is already configured!");
            //throw new Exception("Internal error - Fit Friends object, but DB is already configured");
            return;
        }

        if (instances.size() == 0) {
            DBOpen();

            friendList.clear();
            miataruFriend loc;

            //loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
            //loc.setLocation(1, 1, 5, System.currentTimeMillis());

            //friendList.add(loc);

            // trigger a reload from DB
            new loadFromDB().execute();
        }

        //activeinstances++;
        instances.add(this);
        clean = false;
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
        }

        //activeinstances--;
        instances.remove(this);
        clean = true;

        // If this was the last active object, close the DB
        if (instances.size() == 0) {
            if (MainActivity.Debug > 0) {
                Log.v(LOGTAG, "persistentFriends - Last object destroyed. Closing DB and stopping everything");
            }
            DBClose();
        }
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
            //alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
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

    }

    private void disableFriendPullJob() {
        if (MainActivity.Debug > 2) {
            Log.v(LOGTAG, "persistentFriends - disableFriendPullJob");
        }
        alarmMgr.cancel(alarmIntent);
    }


    //
    //
    // General methods to add remove friends from DB
    //
    //
    public void addFriend(String UUID) {
        this.addFriend(UUID, null);
    }

    public void addFriend(String UUID, String alias) {
        // Check if UUID has some content
        if (UUID == null || UUID.length() == 0) {
            return;
        }
        // Check that we don't have already that UUID
        for (int x = 0; x < friendList.size(); x++) {
            if (friendList.get(x).getUUID().compareTo(UUID) == 0) {
                return;
            }
        }

        // Add new friend to the list
        miataruFriend newFriend = new miataruFriend(UUID, alias);
        friendList.add(newFriend);

        // Add the new object the DB
        this.DBAddFriend(newFriend);
    }

    public void removeFriend(miataruFriend friend) {
        if (friendList.contains(friend)) {
            friendList.remove(friend);
            for (int x = 0; x < callbacks.size(); x++) {
                callbacks.get(x).refreshFriendInfo();
            }
            new deleteFromDB().execute(friend);
        }
    }

    public void removeFriend(String UUID) {
        boolean found = false;
        miataruFriend friend = null;

        for (int x = 0; x < friendList.size(); x++) {
            friend = friendList.get(x);
            if (friend.getUUID().compareTo(UUID) == 0) {
                friendList.remove(x);
                break;
            }
        }

        if (friend != null) {
            //DBRemoveFriend(friend);
            for (int x = 0; x < callbacks.size(); x++) {
                callbacks.get(x).refreshFriendInfo();
            }
            new deleteFromDB().execute(friend);
        }
    }

    public static ArrayList<miataruFriend> getFriends() {
        return friendList;
    }

    //
    //
    // DB access internal funtcions
    //
    //
    private void DBOpen() {
        if (instances.size() == 0) {
            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "Starting new DB instance");
            }
            dbHelper = new storageFriends(MainActivity.getAppContext());
            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "opening a writable DB");
            }
            database = dbHelper.getWritableDatabase();
            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "DB open");
            }
        }
    }

    private void DBClose() {
        if (dbHelper != null && instances.size() == 0) {
            dbHelper.close();
            database = null;
            dbHelper = null;
        }
    }

    private void DBAddFriend(miataruFriend friend) {
        // Add to DB (and remove from "friends" if it could not be added
        // Show a toast in case of error or success

        // Check that the ID doesn't exists
        for (int x = 0; x < friendList.size(); x++) {
            if (friendList.get(x).getUUID().compareTo(friend.getUUID()) == 0) {
                // Duplicate UUID found
                Toast.makeText(MainActivity.getAppContext(), "This ID already exists", Toast.LENGTH_LONG).show();
                return;
            }
        }

        ContentValues values = new ContentValues();
        values.put(storageFriends.COLUMN_UUID, friend.getUUID());
        values.put(storageFriends.COLUMN_SERVER, friend.getServer());
        values.put(storageFriends.COLUMN_ALIAS, friend.getAlias());

        long insertId = database.insert(storageFriends.TABLE_FRIENDS, null, values);

    }

    private void DBRemoveFriend(miataruFriend friend) {

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
        String[] newUUIDs = new String[newFriends.size()];

        for (int x = 0; x < oldFriends.size(); x++) {
            oldUUIDs[x] = oldFriends.get(x).getUUID();
            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "mergeLists - old friend: " + oldUUIDs[x]);
            }
        }
        for (int x = 0; x < newFriends.size(); x++) {
            miataruFriend friend = newFriends.get(x);
            String ID = friend.getUUID();

            if (MainActivity.Debug > 10) {
                Log.v(LOGTAG, "mergeLists - new friend: " + ID);
            }

            newUUIDs[x] = ID;

            boolean newID = true;
            for (int n = 0; n < oldFriendCount; n++) {
                if (ID.compareTo(oldUUIDs[n]) == 0) {
                    // Same UUID found. Copy info in case it has changed.
                    newID = false;
                    if (friend.getAlias() != null && friend.getAlias().length() > 0 && friend.getAlias().compareTo(oldFriends.get(n).getAlias()) != 0) {
                        oldFriends.get(n).setAlias(friend.getAlias());
                        dataChanged = true;
                    }
                    if (friend.hasLocation()) {
                        if (MainActivity.Debug > 10) {
                            Log.v(LOGTAG, "mergeLists - friend exists. adding the info");
                        }

                        oldFriends.get(n).setLocation(friend.getLocation());
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
                    if (MainActivity.Debug > 5) {
                        Log.v(LOGTAG, "mergeLists removing " + oldFriends.get(x).getShowText());
                    }
                    oldFriends.remove(x);
                    dataChanged = true;
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

            try {
                Thread.sleep(3000);                 //1000 milliseconds is one second.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Only for debugging purposes
            //friendList.clear();
            miataruFriend loc;

            loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"), "Myself");
            //loc.setLocation(1,1,5,System.currentTimeMillis());
            internalFriends.add(loc);

            loc = new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216", "Demo Miataru device");
            //loc.setLocation(1, 1, 5, System.currentTimeMillis() - 60 * 60 * 3 * 1000);
            internalFriends.add(loc);

            loc = new miataruFriend("00000000-0000-0000-0000-000000000001", "Mendillorri");
            loc.setLocation(42.813323, -1.612299, Float.valueOf(30));
            internalFriends.add(loc);

            loc = new miataruFriend("00000000-0000-0000-0000-000000000002", "CarlosV");
            loc.setLocation(43.364981, -1.793445, Float.valueOf(30));
            internalFriends.add(loc);

            //loc = new miataruFriend("3dcfbbe1-8018-4a88-acec-9d2aa6643e13", "Test handy");
            //loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis() - 93 * 1000);
            //internalFriends.add(loc);

            internalFriends.add(new miataruFriend("7b8e6e0ee5296db345162dc2ef652c1350761823", "Miataru1"));
            internalFriends.add(new miataruFriend("b0d3c313f97e199eb733e5e9846a3c2c53b4aff4", "Miataru2"));
            internalFriends.add(new miataruFriend("d9faf945cdcb11350bb7a4ccbb2c84138fe4ba54", "Miataru3"));
            internalFriends.add(new miataruFriend("e64a3682ce5a44cff5d9aeaf4c4697c26fa4f977", "Miataru4"));

            loc = new miataruFriend("45E41CC2-84E7-4258-8F75-3BA80CC0E652");
            //loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis() - 60 * 3 * 1000);
            internalFriends.add(loc);


            //loc = new miataruFriend("99999999-9999-9999-9999-999999999999", Long.toString(System.currentTimeMillis()));
            //loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis() - 10 * 1000);
            //internalFriends.add(loc);

            return internalFriends;

        }

        @Override
        protected void onPostExecute(ArrayList<miataruFriend> friendsFromDB) {
            if (MainActivity.Debug > 2) {
                Log.v(LOGTAG, "persistentFriends - Friends loaded from DB: " + friendsFromDB.size());
            }
            tasks.remove(this);
            boolean newData = false;
            // Load the friend list to internal variable
            if (friendList.size() > 0) {
                // There is some data already. compare and look for new
                // We plan to use this call only for initial load, so this is still empty
                if (MainActivity.Debug > 5) {
                    Log.v(LOGTAG, "persistentFriends - Merging " + friendsFromDB.size() + " to " + friendList.size());
                }

                newData = mergeLists(friendList, friendsFromDB, false);
            } else {
                friendList.clear();

                //miataruFriend loc;
                //loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
                //loc.setLocation(1,1,5,System.currentTimeMillis());
                //friendList.add(loc);

                friendList.addAll(friendsFromDB);
                newData = true;
            }

            if (MainActivity.Debug > 5) {
                Log.v(LOGTAG, "persistentFriends - Friend list changed: " + newData);
            }

            // call all the callbacks
            if (newData) {
                for (int x = 0; x < callbacks.size(); x++) {
                    callbacks.get(x).refreshFriendInfo();
                }
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


            try {
                Thread.sleep(1000);                 //1000 milliseconds is one second.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (friendToDelete[0].getAlias() != null && friendToDelete[0].getAlias().compareTo("Test handy") == 0) {
                return null;
            }

            return friend;
        }

        @Override
        protected void onPostExecute(miataruFriend deletedFriend) {
            tasks.remove(this);
            if (deletedFriend == null) {
                Toast.makeText(MainActivity.getAppContext(), "Friend deleted", Toast.LENGTH_SHORT).show();
                //friendList.remove(deletedFriend);
            } else {
                Toast.makeText(MainActivity.getAppContext(), "Friend could not be deleted", Toast.LENGTH_SHORT).show();
                friendList.add(deletedFriend);
                for (int x = 0; x < callbacks.size(); x++) {
                    callbacks.get(x).refreshFriendInfo();
                }
            }
        }
    }

    //
    //
    // Asynchronus tasks for Miataru operations
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
            for (int x = 0; x < callbacks.size(); x++) {
                callbacks.get(x).refreshFriendInfo();
            }
        }

        //this.close();
    }


    //
    //
    // Interface declaration for callbacks
    //
    //
     interface friendEvents {
        void refreshFriendInfo ();
        void refreshFriendLocation ();
        //void refreshFriends ();
    }
}