package com.example.topatu;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Tricky on 21/02/2015.
 */


public class persistentFriends {
    private static int activeinstances = 0;
    private static storageFriends dbHelper = null;
    private static SQLiteDatabase database = null;
    private static ArrayList<miataruFriend> friendList = new ArrayList<miataruFriend>();
    private static ArrayList<friendEvents> callbacks = new ArrayList<friendEvents>();
    private static ArrayList<AsyncTask> tasks = new ArrayList<AsyncTask>();
    private friendEvents myCallback;
    private boolean clean = true;

    private static final String SAVESTATE_NUM = "friendStateCount";
    private static final String SAVESTATE_FRIENDS = "friendStateFriendList";

    public void persistentFriends() throws Exception {
        myCallback = null;

        activeinstances = 8 / 0;

        Log.v("TopatuLog", "Starting new persistentFriends ("+(activeinstances+1)+" in total");

        // If we are the first ones, open the DB
        if (activeinstances == 0 && dbHelper != null) {
            Log.e("TopatuLog", "First Friends object, but DB is already configured!");
            throw new Exception("Internal error - First Friends object, but DB is already configured");
        }

        if (activeinstances == 0) {
            DBOpen();

            miataruFriend loc;
            loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
            loc.setLocation(1,1,5,System.currentTimeMillis());
            friendList.clear();
            friendList.add(loc);

            new loadFromDB().execute();
        }

        activeinstances++;
        clean = false;
    }

    public void persistentFriends(friendEvents callback) throws Exception {
        this.persistentFriends();
        this.registerCallback(callback);
    }

    protected void finalize() {
        if (!clean) {
            this.close();
        }
    }

    public void close() {
        if (myCallback != null) {
            callbacks.remove(myCallback);
            myCallback = null;

            if (callbacks.size() == 0) {
                // This was the last one waiting for any feedback
                // Stop any pending HTTP async tasks
            }
        }

        activeinstances--;
        clean = true;

        // If this was the last active object, close the DB
        if (activeinstances == 0) {
            DBClose();
        }
    }

    public void registerCallback (friendEvents callback) {
        myCallback = callback;
        callbacks.add(myCallback);

        if (callbacks.size() == 1) {
            // This is the firs one interested in life data.
            // Start HTTP async tasks
        }
    }

    public static void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(SAVESTATE_NUM, friendList.size());
        if (friendList.size() > 0) {
            savedInstanceState.putParcelableArrayList(SAVESTATE_FRIENDS, friendList);
        }
    }

    public static void onRestoreInstanceState(Bundle savedInstanceState) {
        if (activeinstances != 0) {
            Log.v("TopatuLog", "ERROR ERROR ERROR ERROR ERROR ERROR ERROR");
            Log.v("TopatuLog", "    Something I didn't understand correctly");
            Log.v("TopatuLog", "ERROR ERROR ERROR ERROR ERROR ERROR ERROR");
            return;
        }
        if (savedInstanceState != null) {
            int numFriends = savedInstanceState.getInt(SAVESTATE_NUM, 0);
            if (numFriends > 0 && savedInstanceState.containsKey(SAVESTATE_FRIENDS)) {
                friendList = savedInstanceState.getParcelableArrayList(SAVESTATE_FRIENDS);
            }
        }

    }

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
            DBRemoveFriend(friend);
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
        if (activeinstances == 0) {
            dbHelper = new storageFriends(MainActivity.getAppContext());
            database = dbHelper.getWritableDatabase();
        }
    }

    private void DBClose() {
        if (dbHelper != null && activeinstances == 0) {
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
    // Asynchronous tasks for DB operations
    //

    // Load full DB
    private class loadFromDB extends AsyncTask<Void, Void, ArrayList<miataruFriend>> {

        @Override
        protected void onPreExecute() {
            tasks.add(this);
        }

        @Override
        protected ArrayList<miataruFriend> doInBackground(Void... none) {
            // Do SQL select and read all the friend info
            ArrayList<miataruFriend> internalFriends = new ArrayList<miataruFriend>();

            // Only for debugging purposes
            //friendList.clear();
            miataruFriend loc;

            //loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
            //loc.setLocation(1,1,5,System.currentTimeMillis());
            //internalFriends.add(loc);

            loc = new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216","Demo Miataru device");
            loc.setLocation(1,1,5,System.currentTimeMillis() - 60*60*3*1000);
            internalFriends.add(loc);

            loc = new miataruFriend("45E41CC2-84E7-4258-8F75-3BA80CC0E652");
            loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis() - 60*3*1000 );
            internalFriends.add(loc);

            loc = new miataruFriend("3dcfbbe1-8018-4a88-acec-9d2aa6643e13","Test handy");
            loc.setLocation(2.0,2.0,50.0,System.currentTimeMillis() - 93*1000);
            internalFriends.add(loc);

            loc = new miataruFriend("99999999-9999-9999-9999-999999999999",Long.toString(System.currentTimeMillis()));
            loc.setLocation(2.0,2.0,50.0,System.currentTimeMillis() - 10*1000);
            internalFriends.add(loc);

            return internalFriends;

        }

        @Override
        protected void onPostExecute(ArrayList<miataruFriend> friendsFromDB) {
            tasks.remove(this);
            boolean newData = false;
            // Load the friend list to internal variable
            if (friendList.size() > 0) {
                // There is some data already. compare and look for new
                // We plan to use this call only for initial load, so this is still empty
                int x = 5 / 0;
            } else {
                friendList.clear();

                miataruFriend loc;
                loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
                loc.setLocation(1,1,5,System.currentTimeMillis());
                friendList.add(loc);

                friendList.addAll(friendsFromDB);
                newData = true;
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
    // Interface declaration for callbacks
    //
    //
     interface friendEvents {
        void refreshFriendInfo ();
        void refreshFriendLocation ();
        //void refreshFriends ();
    }
}