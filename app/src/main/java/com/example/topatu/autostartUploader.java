package com.example.topatu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.util.Calendar;

/**
 * Created by Ixa on 09/02/2015.
 */

public class autostartUploader extends BroadcastReceiver {
    private final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BOOT_COMPLETED_ACTION)) {
            // in our case intent will always be BOOT_COMPLETED, so we can just set
            // the alarm
            // Note that a BroadcastReceiver is *NOT* a Context. Thus, we can't use
            // "this" whenever we need to pass a reference to the current context.
            // Thankfully, Android will supply a valid Context as the first parameter

            //
            // If no UUID is available, application was never started. Don't schedule uploader
            //
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String MyID = settings.getString("my_id", null);
            if (MyID != null) {
                int minutes = settings.getInt("interval",0);
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(context, serviceLocationUploader.class);
                PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
                am.cancel(pi);
                // by my own convention, minutes <= 0 means notifications are disabled
                if (minutes > 0) {
                    am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes * 60 * 1000, minutes * 60 * 1000, pi);
                }
            }
        }
    }
}

