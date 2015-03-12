package com.example.topatu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.IBinder;
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
public class serviceLocationUploader extends Service {
    private final static String LOGTAG = "TopatuUploadLog";
    private int latestInstance;
    private BroadcastReceiver alarmReceiver;

    private static AlarmManager alarmMgr;
    private static PendingIntent alarmIntent;
    private static BroadcastReceiver friendLocationReceiver;


    /**
     * Simply return null, since our Service will not be communicating with
     * any other components. It just does its work silently.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(LOGTAG,"serviceLocationUploader - onCreate");

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Called by periodic alarm
                // Get current location and upload to DB
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        String initiator = intent.getStringExtra("StartedFrom");
        if ( initiator != null ) {
            Log.v(LOGTAG, "serviceLocationUploader - onStartCommand: Service started from " + initiator);
        } else {
            Log.v(LOGTAG,"serviceLocationUploader - onStartCommand: Service started from unknown source");
        }

        latestInstance = startId;

        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }
        if (alarmIntent == null) {
            Intent newIntent = new Intent(MainActivity.getAppContext(), receiverSaveLocation.class);
            alarmIntent = PendingIntent.getBroadcast(MainActivity.getAppContext(), 0, newIntent, 0);
        }
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 5000, alarmIntent);


        return Service.START_NOT_STICKY;
    }

    public void stopService() {
        stopSelfResult(latestInstance);
    }

    @Override
    public void onDestroy() {
        alarmMgr.cancel(alarmIntent);
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
