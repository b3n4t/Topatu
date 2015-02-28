package com.example.topatu;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Tricky on 21/02/2015.
 */


public class persistentFriends {
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

        Log.v(LOGTAG, "persistentFriends - new ");

        Log.v(LOGTAG, "persistentFriends - "+ (instances.size() + 1)+" until now");

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
            loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
            loc.setLocation(1, 1, 5, System.currentTimeMillis());

            //Bundle saco = new Bundle();
            //Log.v(LOGTAG,"PARCELABLE - Saving "+loc.getAlias());
            //saco.putParcelable("Testing",loc);
            //loc = null;
            //loc = saco.getParcelable("Testing");
            //Log.v(LOGTAG,"PARCELABLE - Restored "+loc.getAlias());

            friendList.add(loc);
        }

        //activeinstances++;
        instances.add(this);
        clean = false;

        // trigger a reload from DB
        new loadFromDB().execute();

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
                if ( 0 > 1 ) {
                    // we don't know what we are doing here....
                    return;
                }
            }
        }

        //activeinstances--;
        instances.remove(this);
        clean = true;

        // If this was the last active object, close the DB
        if (instances.size() == 0) {
            Log.v(LOGTAG,"persistentFriends - Last object destroyed. Closing DB and stoping everything");
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
        /*
        if (activeinstances != 0) {
            Log.v("TopatuLog", "ERROR ERROR ERROR ERROR ERROR ERROR ERROR");
            Log.v("TopatuLog", "    Something I didn't understand correctly");
            Log.v("TopatuLog", "ERROR ERROR ERROR ERROR ERROR ERROR ERROR");
            return;
        }
        */
        if (savedInstanceState != null) {
            int numFriends = savedInstanceState.getInt(SAVESTATE_NUM, 0);
            Log.v(LOGTAG,"persistentFriends - onRestoreInstanceState: " + numFriends + " friends in save bundle");
            if (numFriends > 0 && savedInstanceState.containsKey(SAVESTATE_FRIENDS)) {
                friendList.clear();
                ArrayList<miataruFriend> friendsFromSave = savedInstanceState.getParcelableArrayList(SAVESTATE_FRIENDS);
                Log.v(LOGTAG, "persistentFriends - onRestoreInstanceState: " + friendsFromSave.size() + " objects read");

                friendList.addAll(friendsFromSave);
                friendsFromSave = null;
            } else {
                Log.v(LOGTAG,"persistentFriends - onRestoreInstanceState: could not find any object");
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
            Log.v(LOGTAG,"Starting new DB instance");
            dbHelper = new storageFriends(MainActivity.getAppContext());
            Log.v(LOGTAG, "opening a writable DB");
            database = dbHelper.getWritableDatabase();
            Log.v(LOGTAG,"DB open");
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
    private boolean mergeLists(ArrayList<miataruFriend> oldFriends, ArrayList<miataruFriend> newFriends) {
        if ( newFriends == null || oldFriends == null ) { return false; }

        boolean dataChanged = false;

        int oldFriendCount = oldFriends.size();
        String[] oldUUIDs = new String[oldFriendCount];
        String[] newUUIDs = new String[newFriends.size()];

        for (int x=0; x < oldFriends.size();x++) {
            oldUUIDs[x] = oldFriends.get(x).getUUID();
            //Log.v(LOGTAG,"mergeLists - old friend: " + oldUUIDs[x]);
        }
        for (int x=0; x < newFriends.size();x++) {
            miataruFriend friend = newFriends.get(x);
            String ID = friend.getUUID();

            //Log.v(LOGTAG,"mergeLists - new friend: " + ID);

            newUUIDs[x] = ID;

            boolean newID = true;
            for (int n=0;n<oldFriendCount;n++) {
                if ( ID.compareTo(oldUUIDs[n]) == 0 ) {
                    // Same UUID found. Copy info in case it has changed.
                    newID = false;
                    oldFriends.get(n).setAlias(friend.getAlias());
                    if ( friend.hasLocation() ) {
                        //Log.v(LOGTAG,"mergeLists - friend exists. adding the info");

                        oldFriends.get(n).setLocation(friend.getLocation());
                        dataChanged = true;
                    }
                    break;
                }
            }

            if ( newID ) {
                //Log.v(LOGTAG,"mergeLists - new friend. add it");

                oldFriends.add(friend);
                dataChanged = true;
            }
        }

        // Check if some of the friends we have are gone in the new load and delete them
        // Should not happen, just for consistency
        for (int x=oldFriendCount-1;x>=0;x--) {
            String ID = oldFriends.get(x).getUUID();
            boolean missing = true;

            for (int n=0;n<newFriends.size();n++) {
                if ( ID.compareTo(newUUIDs[n]) == 0 ) {
                    missing = false;
                    break;
                }
            }

            if ( missing ) {
                oldFriends.remove(x);
                dataChanged = true;
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
    private class loadFromDB extends AsyncTask<Void, Void, ArrayList<miataruFriend>> {

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
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Only for debugging purposes
            //friendList.clear();
            miataruFriend loc;

            loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
            loc.setLocation(1,1,5,System.currentTimeMillis());
            internalFriends.add(loc);

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
            Log.v(LOGTAG,"persistentFriends - Friends loaded from DB: " + friendsFromDB.size());
            tasks.remove(this);
            boolean newData = false;
            // Load the friend list to internal variable
            if (friendList.size() > 0) {
                // There is some data already. compare and look for new
                // We plan to use this call only for initial load, so this is still empty
                Log.v(LOGTAG,"persistentFriends - Merging " + friendsFromDB.size() + " to " + friendList.size());

                Bundle saco = new Bundle();
                Log.v(LOGTAG,"PARCELABLE - Saving " + friendsFromDB.size() + " friends");
                saco.putParcelableArrayList("Testing",friendsFromDB);
                friendsFromDB = null;
                friendsFromDB = saco.getParcelableArrayList("Testing");
                Log.v(LOGTAG,"PARCELABLE - Restored " + friendsFromDB.size() + " friends");


                newData = mergeLists(friendList,friendsFromDB);


            } else {
                friendList.clear();

                miataruFriend loc;
                loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext()).getString("my_id", "No own UUID!!!!!!"),"Myself");
                loc.setLocation(1,1,5,System.currentTimeMillis());
                friendList.add(loc);

                friendList.addAll(friendsFromDB);
                newData = true;
            }

            Log.v(LOGTAG,"persistentFriends - New data: " + newData);

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
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if ( friendToDelete[0].getAlias() != null && friendToDelete[0].getAlias().compareTo("Test handy") == 0 ) { return null; }

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
    private class MiataruGet extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... deviceids) {
            //String URL = new String("https://"+zerbitzaria+"/v"+bertsioa+"/GetLocation");
            String URL = new String("https://service.miataru.com/v1/GetLocation");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost request = new HttpPost(URL);
            HttpResponse response;
            InputStream buffer;
            String wholeanswer;
            StringEntity postdata;
            JSONObject jsonrequest;

            try {
                jsonrequest = new JSONObject();
                JSONArray devicelist = new JSONArray();
                for (int i=0;i<deviceids.length;i++) {
                    JSONObject device = new JSONObject();
                    device.accumulate("Device", deviceids[i]);
                    devicelist.put(device);
                }
                jsonrequest.accumulate("MiataruGetLocation", devicelist );
                //Log.d(LOGTAG,"Request: "+jsonrequest.toString());
            } catch(Exception e) {
                //Log.d(LOGTAG,e.toString());
                return "{ \"error\": \"Error creating JSON request - "+e.toString()+"\" }";
            }

            try {
                postdata = new StringEntity(jsonrequest.toString(),"UTF-8");
            } catch(Exception e) {
                //Log.d(LOGTAG,e.toString());
                return "{ \"error\": \"String processing error - "+e.toString()+"\" }";
            }
            postdata.setContentType("application/json;charset=UTF-8");
            postdata.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
            //entity = json_string;
            request.setEntity(postdata);

            try {
                response = httpclient.execute(request);
                HttpEntity httpEntity = response.getEntity();
                buffer = httpEntity.getContent();
            } catch(Exception e) {
                //Log.d(LOGTAG,e.toString());
                return "{ \"error\":\"HTTP error - " + e.toString() + "\"}";
            }
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(buffer, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                buffer.close();
                wholeanswer = sb.toString();

            } catch (Exception e) {
                //Log.d(LOGTAG,e.toString());
                return "{ \"error\":\"Error reading HTTP answer - " + e.toString() + "\"}";
            }
            return wholeanswer;

        }
        @Override
        protected void onPostExecute(String result) {
            Log.d("MiataruLog",result);
            //dbgoutput(result);
            //dbgoutput("{\"MiataruGetLocation\":[{\"Device\":\"BF0160F5-4138-402C-A5F0-DEB1AA1F4216\"}]}");
            // Process the JSPON answer
            JSONObject answer;
            JSONArray list;
            try {
                answer = new JSONObject(result);
            } catch (Exception e) {
                Log.d(LOGTAG,"No JSON data could be found on the answer");
            }
            try {
                answer = new JSONObject(result);
                list = answer.getJSONArray("MiataruLocation");
            } catch (Exception e) {
                Log.d(LOGTAG,"No Miataru answer");
                return;
            }
            if ( list != null ) {
                for (int i=0;i<list.length();i++) {
                    String ID = null;
                    try {
                        JSONObject devicepos = list.getJSONObject(i);
                        Location location = new Location("Miataru");

                        ID = devicepos.getString("Device");

                        location.setAltitude(devicepos.getDouble("Latitude"));
                        location.setLongitude(devicepos.getDouble("Longitude"));
                        location.setAccuracy((float)devicepos.getDouble("HorizontalAccuracy"));
                        location.setTime(devicepos.getLong("Timestamp"));
                        //devicelocations.put(ID, location);
                        Log.d(LOGTAG,"Saving "+ID);
                    } catch (Exception e) {
                        if ( ID != null ) {
                            Log.d(LOGTAG, "Error while reading info device location ("+ID+") - "+e.toString());
                        } else {
                            Log.d(LOGTAG, "Error while reading info device location - "+e.toString());
                        }
                    }
                }
            } else {
                Log.d(LOGTAG,"Could not get location array");
            }

            //devicelocations.
        }
        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
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