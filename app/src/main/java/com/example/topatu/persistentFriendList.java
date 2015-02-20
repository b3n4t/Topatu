package com.example.topatu;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Ixa on 19/02/2015.
 */
public class persistentFriendList {
    private static  ArrayList<String> users;

    static public void add (String ID) {
        if ( users == null ) {
            users = new ArrayList<String>();
        }
        users.add(ID);
        Log.v("TopatuLog", "Added " + ID + ", " + users.size() + " in total");
    }

    static public void remove (String ID) {
        users.remove(ID);
        Log.v("TopatuLog","Removed "+ID+", "+users.size()+" in total");
    }
}
