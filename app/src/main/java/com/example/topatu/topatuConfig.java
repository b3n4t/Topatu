package com.example.topatu;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Ixa on 20/03/2015.
 */
public class topatuConfig {
    Context myContext;
    SharedPreferences pref;


    public topatuConfig(Context contextt) {
        myContext = contextt;
        pref = PreferenceManager.getDefaultSharedPreferences(myContext);
    }

    public String getID() {
        return pref.getString(myContext.getString(R.string.settings_my_id), null);
        /*
        String ID = pref.getString(myContext.getString(R.string.settings_my_id), "No own UUID!!!!!!");
        if ( ID.compareTo("No own UUID!!!!!!") == 0 ) {
            return null;
        }
        */
    }

    public String getServer() {
        return pref.getString(myContext.getString(R.string.settings_my_server), myContext.getString(R.string.settings_my_server_default));

    }

    public String getServerDefault() {
        return myContext.getString(R.string.settings_my_server_default);
    }

    public boolean getStartOnBoot () {
        return pref.getBoolean(myContext.getString(R.string.settings_upload_autostart),false);
    }

    public boolean getUploadLocation () {
        return pref.getBoolean(myContext.getString(R.string.settings_upload_location),false);
    }

    public boolean getSaveHistory () {
        return pref.getBoolean(myContext.getString(R.string.settings_save_location),false);
    }

    public long getIntervalLocationSave () {
        return Long.valueOf(pref.getString(myContext.getString(R.string.settings_save_freq), myContext.getString(R.string.settings_save_freq_default)));
    }

    public long getIntervalLocationUpload () {
        return Long.valueOf(pref.getString(myContext.getString(R.string.settings_upload_freq), myContext.getString(R.string.settings_upload_freq_default)));
    }

    public long getIntervalFriendsRefresh () {
        return 5;
    }
}
