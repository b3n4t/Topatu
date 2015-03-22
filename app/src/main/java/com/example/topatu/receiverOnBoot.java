package com.example.topatu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Ixa on 11/03/2015.
 */
public class receiverOnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        topatuConfig config = new topatuConfig(context);

        // Only start service if user says so
        if ( config.getUploadLocation() && config.getStartOnBoot() ) {
            Intent service = new Intent(context, serviceLocationUploader.class);
            service.putExtra("StartedFrom", "receiverOnBoot");
            context.startService(service);
        }
    }
}
