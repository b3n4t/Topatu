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
        Intent service = new Intent(context, serviceLocationUploader.class);
        service.putExtra("StartedFrom","receiverOnBoot");
        context.startService(service);
    }
}
