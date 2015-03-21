package com.example.topatu;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.IBinder;
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

/**
 * Created by Ixa on 09/02/2015.
 */
public class serviceLocationUploader extends Service {
    private final static String LOGTAG = "TopatuUploadLog";

    private static NotificationManager notificationMgr;
    private static AlarmManager alarmMgr;
    private static PendingIntent alarmIntent;
    private static BroadcastReceiver friendLocationReceiver;

    private int latestInstance;
    private long latestSavedlocation = 0;
    private boolean serviceIsActive = false;

    private topatuConfig config;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(LOGTAG, "serviceLocationUploader - onCreate");

        config = new topatuConfig(this);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIsActive = true;

        //TODO do something useful
        String initiator = intent.getStringExtra("StartedFrom");

        if ( initiator != null ) {
            Log.v(LOGTAG, "serviceLocationUploader - onStartCommand: Service started from " + initiator);
        } else {
            Log.v(LOGTAG,"serviceLocationUploader - onStartCommand: Service started from unknown source");
        }

        latestInstance = startId;

        //receiverSaveLocation
        //serviceLocationUploader;SaveTimeStamp;
        if ( initiator != null && initiator.compareTo("receiverSaveLocation") == 0 ) {
            Long t = intent.getLongExtra("SaveTimeStamp",0);
            if ( t > 0 ) {
                reportSuccessfullSave(t);
            }
        } else {
            if (alarmMgr == null) {
                alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            }
            if (alarmIntent == null) {
                Intent newIntent = new Intent(this, receiverSaveLocation.class);
                alarmIntent = PendingIntent.getBroadcast(this, 0, newIntent, 0);
            }
            //alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 30000, alarmIntent);
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, config.getIntervalLocationSave() * 1000, alarmIntent);


            //
            // Show permanent entry in Notification Bar
            //

            //Intent intent = new Intent(MainActivity.getAppContext(), receiverPullFriendData.class);
            //alarmIntent = PendingIntent.getBroadcast(MainActivity.getAppContext(), 0, intent, 0);
            if (notificationMgr == null) {
                notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }
            Notification not = new Notification(R.drawable.ic_launcher, this.getString(R.string.notification_enabled), System.currentTimeMillis());
            //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, main.class), Notification.FLAG_ONGOING_EVENT);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            not.flags = Notification.FLAG_ONGOING_EVENT;
            //not.setLatestEventInfo(this, "Application Name", "Application Description", contentIntent);
            not.setLatestEventInfo(this, this.getString(R.string.notification_text), this.getString(R.string.notification_longtext), contentIntent);
            notificationMgr.notify(1, not);

        }

        //return Service.START_NOT_STICKY;
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        serviceIsActive = false;
        alarmMgr.cancel(alarmIntent);
        notificationMgr.cancel(1);
    }

    private void reportSuccessfullSave ( long timeStamp ) {
        if ( latestSavedlocation == 0 ) {
            latestSavedlocation = timeStamp;
        } else {
            Log.v(LOGTAG,"serviceLocationUploader - reportSuccessfullSave - " + (timeStamp - latestSavedlocation) + " (" + config.getIntervalLocationUpload() + ")");
            if ( timeStamp - latestSavedlocation > config.getIntervalLocationUpload() * 1000 ) {

                //String myID = PreferenceManager.getDefaultSharedPreferences(this).getString(this.getString(R.string.settings_my_id), "No own UUID!!!!!!");
                //String myServer = PreferenceManager.getDefaultSharedPreferences(this).getString(this.getString(R.string.settings_my_server), this.getString(R.string.settings_my_server_default));
                String myID = config.getID();
                String myServer = config.getServer();

                if ( myID == null || myID.compareTo("No own UUID!!!!!!") == 0 ) { return; }

                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));

                if ( location == null ) { return; }

                miataruFriend me = new miataruFriend(myID,myServer);
                //location.setTime(System.currentTimeMillis());
                //location.setTime(timeStamp);
                me.setLocation(location);

                new MiataruUpload().execute(me);

                latestSavedlocation = timeStamp;
            }
        }
    }

    //
    //
    // AsyncTask to upload data to internet
    //
    //
    private class MiataruUpload extends AsyncTask<miataruFriend,Void,String> {
        @Override
        protected String doInBackground(miataruFriend... params) {
            String URL = new String("https://service.miataru.com/v1/UpdateLocation");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost request = new HttpPost(URL);
            HttpResponse response;
            InputStream buffer;
            String wholeanswer;
            StringEntity postdata;
            JSONObject jsonrequest;

            miataruFriend me = params[0];

            if ( me == null || !me.hasLocation() ) { return "Error: No location"; }

            try {
                if ( me == null ) { return ""; }

                URL = "https://" + me.getServer() + "/v1/UpdateLocation";

                JSONObject miataruheader = new JSONObject();
                request = new HttpPost(URL);

                //miataruheader.accumulate("EnableLocationHistory", "True");
                //miataruheader.accumulate("LocationDataRetentionTime", String.valueOf(15));

                miataruheader.accumulate("EnableLocationHistory", "True");
                miataruheader.accumulate("LocationDataRetentionTime", "1440");

                jsonrequest = new JSONObject();
                jsonrequest.accumulate("MiataruConfig", miataruheader);

                JSONArray jsonlocationlist = new JSONArray();

                JSONObject jsonlocation = new JSONObject();
                jsonlocation.accumulate("Device", me.getUUID());
                /*
                jsonlocation.accumulate("Timestamp", String.valueOf(me.getTimeStamp()));
                jsonlocation.accumulate("Longitude", String.valueOf(me.getLongitude()));
                jsonlocation.accumulate("Latitude", String.valueOf(me.getLatitude()));
                //jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(me.getAccuracy()));
                jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(me.getAccuracy()));
                */
                jsonlocation.accumulate("Timestamp", String.valueOf(Long.valueOf(me.getTimeStamp()/1000)));
                jsonlocation.accumulate("Longitude", String.valueOf(me.getLongitude()));
                jsonlocation.accumulate("Latitude", String.valueOf(me.getLatitude()));
                jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(me.getAccuracy()));
                //jsonlocation.accumulate("Altitude", String.valueOf(me.getAltitude()));

                jsonlocationlist.put(jsonlocation);

                jsonrequest.accumulate("MiataruLocation", jsonlocationlist );


            } catch(Exception e) {
                //Log.d(LOGTAG, e.toString());
                //return "{ \"error\": \"Error creating JSON request - "+e.toString()+"\" }";
                return "Error: Error creating JSON request";
            }

            //Log.d(LOGTAG,"Upload request: "+jsonrequest.toString());
            //dbgoutput(jsonrequest.toString());


            //Log.v(LOGTAG,"JSON: " + jsonrequest.toString());

            try {
                postdata = new StringEntity(jsonrequest.toString(),"UTF-8");
            } catch(Exception e) {
                //Log.d(LOGTAG,e.toString());
                //return "{ \"error\": \"String processing error - "+e.toString()+"\" }";
                return "Error: String processing error";
            }
            postdata.setContentType("application/json;charset=UTF-8");
            postdata.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
            //entity = json_string;
            request.setEntity(postdata);

            // Do the HTTP call
            try {
                response = httpclient.execute(request);
                HttpEntity httpEntity = response.getEntity();
                buffer = httpEntity.getContent();
            } catch(Exception e) {
                //Log.d(LOGTAG,e.toString());
                //return "{ \"error\":\"HTTP error - " + e.toString() + "\"}";
                return "Error: HTTP error";
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
                //return "{ \"error\":\"Error reading HTTP answer - " + e.toString() + "\"}";
                return "Error: Error reading HTTP answer";
            }
            //return jsonrequest.toString()+"\n"+wholeanswer;
            //return wholeanswer;


            //Log.v(LOGTAG,"JSON: " + wholeanswer );

            JSONObject answer;
            //JSONArray list;
            String answerCode;
            String answerDetails;

            try {
                answer = new JSONObject(wholeanswer);
            } catch (Exception e) {
                //Log.d(LOGTAG,"No JSON data could be found on the answer");
                //return null;
                return "Error: No JSON data could be found on the answer";
            }
            try {
                answerCode = answer.getString("MiataruResponse");
            } catch (Exception e) {
                //Log.d(LOGTAG,"No Miataru answer");
                //return null;
                return "Error: No MiataruResponse found.";
            }
            try {
                answerDetails = answer.getString("MiataruVerboseResponse");
            } catch (Exception e) {
                //Log.d(LOGTAG,"No Miataru answer");
                //return null;
                return "Error: No MiataruVerboseResponse found.";
            }
            if ( answerCode.compareTo("ACK") == 0 ) {
                //return "Location uploaded ("+answerDetails+")";
                latestSavedlocation = me.getTimeStamp();
                return serviceLocationUploader.this.getString(R.string.showtext_location_uploaded);
            } else if ( answerCode.compareTo("NACK") == 0 ) {
                return "Error: "+answerDetails;
            } else {
                return "Error: Unknown answer - "+answerCode+"("+answerDetails+")";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.v(LOGTAG, "serviceIsActive - MiataruUpload - onPostExecute");
            if ( ! result.startsWith(serviceLocationUploader.this.getString(R.string.showtext_location_uploaded)) ) {
                Toast.makeText(serviceLocationUploader.this, result, Toast.LENGTH_LONG).show();
            } else if ( MainActivity.Debug > 0 ) {
                Toast.makeText(serviceLocationUploader.this, result, Toast.LENGTH_LONG).show();
            }
            //Log.d(LOGTAG,result);
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
