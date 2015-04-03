package com.example.topatu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Tricky on 21/02/2015.
 */
public class storageFriends extends SQLiteOpenHelper {

    public static final String TABLE_FRIENDS = "friends";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_SERVER = "server";
    public static final String COLUMN_ALIAS = "alias";
/*
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_TIMESTAMP = "timestamp";
*/
    private static final String DATABASE_NAME = "friends.db";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table " + TABLE_FRIENDS + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_UUID + " text not null, "
            + COLUMN_SERVER + " text not null, "
            + COLUMN_ALIAS + " text );";

    public storageFriends(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.v("TopatuLog", "Creating Friend DB");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("TopatuLog",
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        //onCreate(db);
        if ( oldVersion < 2 ) {
            // upgrade from 1 to 2
            //internalFriends.add(new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216", "service.miataru.com", "Demo Miataru device"));
            topatuConfig config = new topatuConfig(MainActivity.getAppContext());
            db.execSQL("UPDATE " + TABLE_FRIENDS + " SET " + COLUMN_SERVER + " = '" + config.getServerDefault() + "' WHERE " + COLUMN_SERVER + " IS NULL");
            db.execSQL("INSERT INTO " + TABLE_FRIENDS + " (" + COLUMN_UUID + "," + COLUMN_SERVER + "," + COLUMN_ALIAS + ") VALUES ('BF0160F5-4138-402C-A5F0-DEB1AA1F4216','service.miataru.com','Demo Miataru device')");
        }
        if ( oldVersion < 3 ) {
            // upgrade from 2 to 3
        }
    }
}
