package com.example.topatu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.ArrayList;

/**
 * Created by Ixa on 02/03/2015.
 */
public class receiverPullFriendData extends BroadcastReceiver {
    private final static String LOGTAG = "TopatuLog";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MainActivity.Debug > 3) { Log.v("TopatuLog", "receiverPullFriendData - onReceive " + intent.getAction()); }

        new MiataruGet().execute(persistentFriends.getFriends());
    }

    private class MiataruGet extends AsyncTask<ArrayList<miataruFriend>,Void,ArrayList<miataruFriend>> {
        @Override
        protected ArrayList<miataruFriend> doInBackground(ArrayList<miataruFriend>... params ) {

            ArrayList<miataruFriend> friendList = params[0];

            ArrayList<miataruFriend> friendLocations = new ArrayList<miataruFriend>();

            //ArrayList<miataruFriend> friendList = persistentFriends.getFriends();

            String URL = new String("http://service.miataru.com/v1/GetLocation");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost request = new HttpPost(URL);
            HttpResponse response;
            InputStream buffer;
            String wholeanswer;
            StringEntity postdata;
            JSONObject jsonrequest;

            try {
                jsonrequest = new JSONObject();
                JSONArray devicelist = new JSONArray();
                for (int i=0;i<friendList.size();i++) {
                    JSONObject device = new JSONObject();
                    device.accumulate("Device", friendList.get(i).getUUID());
                    devicelist.put(device);
                }
                jsonrequest.accumulate("MiataruGetLocation", devicelist );
                //Log.d(LOGTAG,"Request: "+jsonrequest.toString());
            } catch(Exception e) {
                Log.d(LOGTAG,"Error creating the JSON request: " + e.toString());
                return null;
                //return "{ \"error\": \"Error creating JSON request - "+e.toString()+"\" }";
            }

            try {
                postdata = new StringEntity(jsonrequest.toString(),"UTF-8");
            } catch(Exception e) {
                Log.d(LOGTAG,"Error creating the HTTP POST request: " + e.toString());
                return null;
                //return "{ \"error\": \"String processing error - "+e.toString()+"\" }";
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
                Log.d(LOGTAG,"Error getting the HTTP request: " + e.toString());
                return null;
                //return "{ \"error\":\"HTTP error - " + e.toString() + "\"}";
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
                Log.d(LOGTAG,"Error reading HTTP answer: " + e.toString());
                return null;
                //return "{ \"error\":\"Error reading HTTP answer - " + e.toString() + "\"}";
            }

            //
            // Process the answer
            //
            //Log.d(LOGTAG,"************************************");
            //Log.d(LOGTAG,"JSON answer");
            //Log.d(LOGTAG,wholeanswer);
            //Log.d(LOGTAG,"************************************");
            JSONObject answer;
            JSONArray list;
            try {
                answer = new JSONObject(wholeanswer);
            } catch (Exception e) {
                Log.d(LOGTAG,"No JSON data could be found on the answer");
                return null;
            }
            try {
                list = answer.getJSONArray("MiataruLocation");
            } catch (Exception e) {
                Log.d(LOGTAG,"No Miataru answer");
                return null;
            }
            if ( list != null ) {
                for (int i=0;i<list.length();i++) {
                    String ID = null;
                    try {
                        if ( list.isNull(i)  ) { continue; }

                        JSONObject devicepos = list.getJSONObject(i);
                        Location location = new Location("Miataru");

                        ID = devicepos.getString("Device");

                        location.setLatitude(devicepos.getDouble("Latitude"));
                        location.setLongitude(devicepos.getDouble("Longitude"));
                        location.setAccuracy((float)devicepos.getDouble("HorizontalAccuracy"));
                        location.setTime(devicepos.getLong("Timestamp")*1000);
                        if ( ! devicepos.isNull("Altitude")) {
                            location.setAltitude(devicepos.getDouble("Altitude"));
                        }

                        miataruFriend friendLoc = new miataruFriend(ID);
                        friendLoc.setLocation(location);

                        //devicelocations.put(ID, location);
                        if ( MainActivity.Debug > 5 ) { Log.d(LOGTAG,"Friend location for "+ID); }
                        friendLocations.add(friendLoc);
                    } catch (Exception e) {
                        if ( ID != null ) {
                            Log.d(LOGTAG, "Error while reading info device location ("+ID+") - "+e.toString());
                        } else {
                            Log.d(LOGTAG, "Error while reading info device location - "+e.toString());
                        }
                    }
                }
            } else {
                Log.d(LOGTAG,"Could not get location array");
            }

            return friendLocations;
        }

        protected void onPostExecute(ArrayList<miataruFriend> friendLocations) {

            if ( friendLocations == null ) {
                Toast.makeText(MainActivity.getAppContext(), "Error refreshing friend location", Toast.LENGTH_LONG).show();
            } else if ( friendLocations.size() > 0 ) {
                // Return the friend locations
                Bundle b = new Bundle();
                b.putParcelableArrayList("FriendInfo", friendLocations);
                Intent returnIntent = new Intent();
                returnIntent.setAction("com.example.topatu.friendlocationinformation");
                returnIntent.putExtras(b);
                MainActivity.getAppContext().sendBroadcast(returnIntent);
            }
        }
    }
}