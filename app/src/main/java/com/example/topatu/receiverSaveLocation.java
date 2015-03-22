package com.example.topatu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Ixa on 12/03/2015.
 */
public class receiverSaveLocation extends BroadcastReceiver {
    private final static String LOGTAG = "TopatuUploadLog";
    private Context mycontext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MainActivity.Debug > 3) {
            Log.v(LOGTAG, "receiverUploadLocation - onReceive " + intent.getAction());
        }

        mycontext = context;

        topatuConfig config = new topatuConfig(context);

        String myID = config.getID();
        String myServer = config.getServer();

        if ( myID == null || myID.compareTo("No own UUID!!!!!!") == 0 ) { return; }

        //
        //
        //

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));

        if (location == null) {
            return;
        }
        location.setTime(System.currentTimeMillis());

        miataruFriend me = new miataruFriend(myID, myServer);
        me.setLocation(location);

        new MiataruSave().execute(me);
    }

    //
    //
    //
    //
    //
    private class MiataruSave extends AsyncTask<miataruFriend, Void, Long> {
        @Override
        protected Long doInBackground(miataruFriend... params) {
            topatuConfig config = new topatuConfig(mycontext);

            return params[0].getTimeStamp();
        }

        @Override
        protected void onPostExecute(Long result) {
            Log.v(LOGTAG, "receiverSaveLocation - MiataruSave - onPostExecute");

            Intent service = new Intent(mycontext, serviceLocationUploader.class);
            service.putExtra("StartedFrom","receiverSaveLocation");
            //service.putExtra("SaveTimeStamp",result);
            service.putExtra("SaveTimeStamp",System.currentTimeMillis());
            mycontext.startService(service);
        }
    }
}
