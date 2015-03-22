package com.example.topatu;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Ixa on 19/02/2015.
 */
public class miataruFriend implements Parcelable {
    private String UUID;
    private String Server;
    private String Alias = null;
    private boolean hasLocation = false;
    private double Latitude;
    private double Longitude;
    private double Altitude;
    private float Accuracy;
    private long TimeStamp = -1;

    //
    //
    // Constructors
    //
    //
    public miataruFriend(String UUID, String Server) {
        if (UUID == null || UUID.length() == 0) {
            throw new IllegalArgumentException("Invalid UUID");
        }
        this.UUID = UUID;
        if ( Server != null || Server.length() > 0 ) {
            this.Server = Server;
        } else {
            Server = "service.miataru.com";
        }
    }

    public miataruFriend(String UUID, String Server, String Alias) {
        this(UUID, Server);
        this.Alias = Alias;
    }

    public miataruFriend(String UUID, String Server, String Alias, Location location) {
        this(UUID, Server, Alias);
        this.setLocation(location);
    }

    //
    //
    // Methods to implement parcelable
    //
    //
    protected  miataruFriend(Parcel in) {
        if ( MainActivity.Debug > 8 ) { Log.v("TopatuLog","miataruFriend - creating from PARCEL"); }
        UUID = in.readString();
        Server = in.readString();
        Alias = in.readString();

        hasLocation = in.readInt() != 0;
        if ( hasLocation ) {
            Latitude = in.readDouble();
            Longitude = in.readDouble();
            Altitude = in.readDouble();
            Accuracy = in.readFloat();
            TimeStamp = in.readLong();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if ( MainActivity.Debug > 8 ) { Log.v("TopatuLog", "miataruFriend - writing to PARCEL"); }

        dest.writeString(UUID);
        dest.writeString(Server);
        dest.writeString(Alias);

        dest.writeInt(hasLocation ? 1 : 0);
        if ( hasLocation ) {
            dest.writeDouble(Latitude);
            dest.writeDouble(Longitude);
            dest.writeDouble(Altitude);
            dest.writeFloat(Accuracy);
            dest.writeLong(TimeStamp);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Parcelable.Creator<miataruFriend> CREATOR = new Parcelable.Creator<miataruFriend>() {
        public miataruFriend createFromParcel(Parcel in) {
            return new miataruFriend(in);
        }

        public miataruFriend[] newArray(int size) {
            return new miataruFriend[size];
        }
    };


    //
    //
    // special get functions useful for showing the information
    //
    //
    public String getShowText () {
        if ( Alias != null && Alias.length() > 0 ) {
            return Alias;
        } else {
            return UUID;
        }
    }

    public String getSecondaryText () {
        if ( this.hasLocation() ) {
            //return "Lat " + Latitude + "; Lon: " + Longitude + "  ("+ Accuracy +")";
            return String.format( "Lon: %.2f; Lat: %.2f (%.0f)", Longitude, Latitude, Accuracy );
        }
        if ( Alias != null && Alias.length() > 0 ) {
            return UUID;
        }
        return "";
    }

    public String getLongDescription () {
        String text;
        text = UUID + "\nOn server: "+Server;
        if ( Alias != null && Alias.length() > 0 ) {
            text = text + "\nAlias: " + Alias;
        }
        if ( hasLocation ) {
            text = text + "\n" + String.format( "Lon: %.2f; Lat: %.2f (%.0f)", Longitude, Latitude, Accuracy );
        }
        text = text + "\nLast updated: " + getUpdateTime() + "(" + TimeStamp + ")";

        return text;
    }

    public String getUpdateTime () {
        if ( ! hasLocation || TimeStamp < 0 ) { return "N/A"; }

        long timediff  = (System.currentTimeMillis() - TimeStamp) / 1000;

        String answer;

        //if ( timediff < 0 ) { return "N/A (neg)"; }
        //if ( timediff < 0 ) { answer = "- "; }
        //else { answer = ""; }

        if ( timediff > 60*60*24*2 ) {
            timediff = timediff / (60*60*24);
            answer = timediff+" d";
        } else if ( timediff > 60*60*2 ) {
            timediff = timediff / (60*60);
            answer = timediff+" h";
        } else if ( timediff > 60*2 ) {
            timediff = timediff / 60;
            answer = timediff+" m";
        } else {
            answer = timediff+" s";
        }
        return answer;
    }

    //
    //
    // getters and putters
    //
    //
    public String getUUID() {
        return UUID;
    }

    public String getServer () { return Server; }

    public String getAlias() {
        return Alias;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setServer(String Server) {
        this.Server = Server;
    }

    public void setAlias(String Alias) {
        this.Alias = Alias;
    }

    public boolean hasLocation () {
        return hasLocation;
    }

    public double getLatitude () { return Latitude; }

    public double getLongitude () { return Longitude; }

    public double getAltitude () { return Altitude; }

    public float getAccuracy () { return Accuracy; }

    public long getTimeStamp () { return TimeStamp; }

    //
    //
    // Methods to me used with LocationProvider, GoogleMaps and such
    //
    //
    public void setLocation (double lat, double lon, float accu) {
        this.hasLocation = true;
        this.Latitude = lat;
        this.Longitude = lon;
        this.Accuracy = accu;
    }

    public void setLocation (double lat, double lon, double accu, long time) {
        this.hasLocation = true;
        this.Latitude = lat;
        this.Longitude = lon;
        this.Accuracy = (float) accu;
        this.TimeStamp = time;
    }

    public void setLocation (double lat, double lon, long time) {
        this.hasLocation = true;
        this.Latitude = lat;
        this.Longitude = lon;
        this.TimeStamp = time;
    }

    public void setLocation (double lat, double lon) {
        this.hasLocation = true;
        this.Latitude = lat;
        this.Longitude = lon;
    }

    public void setLocation (LatLng pos) {
        this.hasLocation = true;
        this.Latitude = pos.latitude;
        this.Longitude = pos.longitude;
    }

    public void setLocation (Location location) {
        if ( location == null ) { return; }

        this.hasLocation = true;
        this.Latitude = location.getLatitude();
        this.Longitude = location.getLongitude();
        if (location.hasAccuracy()) {
            this.Accuracy = location.getAccuracy();
        }
        if ( location.hasAltitude() ) {
            this.Altitude = location.getAltitude();
        }
        this.TimeStamp = location.getTime();
    }

    public Location getLocation() {
        if ( hasLocation ) {
            Location loc = new Location("Miataru");
            loc.setLatitude(Latitude);
            loc.setLongitude(Longitude);
            loc.setTime(TimeStamp);
            if (Accuracy > 0 ) {
               loc.setAccuracy(Accuracy);
            }

            return loc;
        } else {
            return null;
        }
    }

    public LatLng getLatLng() {
        if ( hasLocation ) {
            return new LatLng(this.Latitude, this.Longitude);
        } else {
            return null;
        }
    }

    public CircleOptions getCircleOptions() {
        if ( hasLocation ) {
            CircleOptions options = new CircleOptions();
            options.center(getLatLng());
            options.radius(this.Accuracy);

            return options;
        } else {
            return null;
        }
    }

    public MarkerOptions getMarkerOptions() {
        if ( hasLocation ) {
            MarkerOptions options = new MarkerOptions();

            options.position(getLatLng());

            if (Alias != null && Alias.compareTo(UUID) != 0) {
                options.title(Alias);
                options.snippet(UUID);
            } else {
                options.title(UUID);
            }

            return options;
        } else {
            return null;
        }
    }
}
