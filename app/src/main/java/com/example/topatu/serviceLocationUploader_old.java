package com.example.topatu;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

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

/**
 * Created by Ixa on 09/02/2015.
 */
public class serviceLocationUploader_old extends Service {
    private final static String LOGTAG = "TopatuUploadLog";
    private PowerManager.WakeLock mWakeLock;
    /**
     * Simply return null, since our Service will not be communicating with
     * any other components. It just does its work silently.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is where we initialize. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */

    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationUploader");
        mWakeLock.acquire();
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) {
            stopSelf();
            return;
        }

        // do the actual work, in a separate thread
        new UploadTask().execute();
    }

    private class UploadTask extends AsyncTask<Void, Void, Void> {
        /**
         * This is where YOU do YOUR work. There's nothing for me to write here
         * you have to fill this in. Make your HTTP request(s) or whatever it is
         * you have to do to get your updates in here, because this is run in a
         * separate thread
         */
        @Override
        protected Void doInBackground(Void... params) {
            // do stuff! return null;
            return null;
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager.
         *
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         * can go to sleep again and save precious battery.
         */
        @Override
        protected void onPostExecute(Void result) {
            // handle your data
            stopSelf();
        }
    }

    /**
     * This is deprecated, but you have to implement it if you're planning on
     * supporting devices with an API level lower than 5 (Android 2.0).
     */
    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent); return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }

    private class MiataruPut extends AsyncTask<Void,Void,String> {
        @Override
        protected String doInBackground(Void... nothing) {
            //String URL = new String("https://"+zerbitzaria+"/v"+bertsioa+"/UpdateLocation");
            String URL = new String("https://service.miataru.com/v1/UpdateLocation");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost request = new HttpPost(URL);
            HttpResponse response;
            InputStream buffer;
            String wholeanswer;
            StringEntity postdata;
            JSONObject jsonrequest;

            try {
                //Location whereami = devicelocations.get("Own");
                Location whereami = new Location("Miatatu");
                whereami.setLatitude(1.0);
                whereami.setLongitude(1.0);
                if ( whereami == null ) { return ""; }

                JSONObject miataruheader = new JSONObject();
                //miataruheader.accumulate("EnableLocationHistory", String.valueOf(zaharrakgorde));
                miataruheader.accumulate("EnableLocationHistory", "True");
                miataruheader.accumulate("LocationDataRetentionTime", String.valueOf(15));

                jsonrequest = new JSONObject();
                jsonrequest.accumulate("MiataruConfig", miataruheader);

                JSONArray jsonlocationlist = new JSONArray();

                JSONObject jsonlocation = new JSONObject();
                jsonlocation.accumulate("Device", "blabla");
                jsonlocation.accumulate("Timestamp", String.valueOf(whereami.getTime()));
                jsonlocation.accumulate("Longitude", String.valueOf(whereami.getLongitude()));
                jsonlocation.accumulate("Latitude", String.valueOf(whereami.getLatitude()));
                //jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(whereami.getAccuracy()));
                jsonlocation.accumulate("HorizontalAccuracy", "10.0");

                jsonlocationlist.put(jsonlocation);

                jsonrequest.accumulate("MiataruLocations", jsonlocationlist );


            } catch(Exception e) {
                Log.d(LOGTAG, e.toString());
                return "{ \"error\": \"Error creating JSON request - "+e.toString()+"\" }";
            }

            Log.d(LOGTAG,"Upload request: "+jsonrequest.toString());
            //dbgoutput(jsonrequest.toString());


            try {
                postdata = new StringEntity(jsonrequest.toString(),"UTF-8");
            } catch(Exception e) {
                Log.d(LOGTAG,e.toString());
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
                Log.d(LOGTAG,e.toString());
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
                Log.d(LOGTAG,e.toString());
                return "{ \"error\":\"Error reading HTTP answer - " + e.toString() + "\"}";
            }
            return jsonrequest.toString()+"\n"+wholeanswer;
            //return wholeanswer;
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d(LOGTAG,result);
            //dbgoutput(result);
            //dbgoutput("{\"MiataruGetLocation\":[{\"Device\":\"BF0160F5-4138-402C-A5F0-DEB1AA1F4216\"}]}");
            // Process the JSPON answer
        }
        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
