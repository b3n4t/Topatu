package com.example.topatu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
 * Created by Ixa on 12/03/2015.
 */
public class receiverSaveLocation extends BroadcastReceiver {
    private final static String LOGTAG = "TopatuUploadLog";
    private Context mycontext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MainActivity.Debug > 3) { Log.v(LOGTAG, "receiverUploadLocation - onReceive " + intent.getAction()); }

        mycontext = context;

        String myID = PreferenceManager.getDefaultSharedPreferences(context).getString("my_id", "No own UUID!!!!!!");

        if ( myID.compareTo("No own UUID!!!!!!") == 0 ) { return; }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));

        if ( location == null ) { return; }

        miataruFriend me = new miataruFriend(myID);
        me.setLocation(location);

        new MiataruPut().execute(me);
    }

    private class MiataruPut extends AsyncTask<miataruFriend,Void,String> {
        @Override
        protected String doInBackground(miataruFriend... params) {
            //String URL = new String("https://"+zerbitzaria+"/v"+bertsioa+"/UpdateLocation");
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
                //Location whereami = devicelocations.get("Own");
                Location whereami = new Location("Miatatu");
                whereami.setLatitude(1.0);
                whereami.setLongitude(1.0);
                if ( whereami == null ) { return ""; }

                JSONObject miataruheader = new JSONObject();

                //miataruheader.accumulate("EnableLocationHistory", "True");
                //miataruheader.accumulate("LocationDataRetentionTime", String.valueOf(15));

                miataruheader.accumulate("EnableLocationHistory", "False");
                miataruheader.accumulate("LocationDataRetentionTime", "10000");

                jsonrequest = new JSONObject();
                jsonrequest.accumulate("MiataruConfig", miataruheader);

                JSONArray jsonlocationlist = new JSONArray();

                JSONObject jsonlocation = new JSONObject();
                jsonlocation.accumulate("Device", me.getUUID());
                jsonlocation.accumulate("Timestamp", String.valueOf(me.getTimeStamp()));
                jsonlocation.accumulate("Longitude", String.valueOf(me.getLongitude()));
                jsonlocation.accumulate("Latitude", String.valueOf(me.getLatitude()));
                //jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(whereami.getAccuracy()));
                jsonlocation.accumulate("HorizontalAccuracy", String.valueOf(me.getAccuracy()));

                jsonlocationlist.put(jsonlocation);

                jsonrequest.accumulate("MiataruLocations", jsonlocationlist );


            } catch(Exception e) {
                //Log.d(LOGTAG, e.toString());
                //return "{ \"error\": \"Error creating JSON request - "+e.toString()+"\" }";
                return "Error: Error creating JSON request";
            }

            //Log.d(LOGTAG,"Upload request: "+jsonrequest.toString());
            //dbgoutput(jsonrequest.toString());


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
                return "Location uploaded ("+answerDetails+")";
            } else if ( answerCode.compareTo("NACK") == 0 ) {
                return "Error: "+answerDetails;
            } else {
                return "Error: Unknown answer - "+answerCode+"("+answerDetails+")";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText( mycontext, result, Toast.LENGTH_LONG).show();
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
