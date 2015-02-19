package com.example.topatu;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Ixa on 19/02/2015.
 */
public class miataruFriend implements Parcelable {
    private String UUID;
    private String Alias = null;
    private boolean hasLocation = false;
    private double Latitude;
    private double Longitude;
    private float Accuracy = 0;
    private long TimeStamp;

    //
    //
    // Constructors
    //
    //
    public miataruFriend(String UUID) {
        if (UUID == null || UUID.length() == 0) {
            throw new IllegalArgumentException("Invalid UUID");
        }
        this.UUID = UUID;
    }

    public miataruFriend(String UUID, String Alias) {
        this(UUID);
        this.Alias = Alias;
    }

    public miataruFriend(String UUID, String Alias, Location location) {
        this(UUID);
        this.Alias = Alias;

        this.hasLocation = true;
        this.Latitude = location.getLatitude();
        this.Longitude = location.getLongitude();
        if (location.hasAccuracy()) {
            this.Accuracy = location.getAccuracy();
        }
        this.TimeStamp = location.getTime();
    }

    //
    //
    // Methods to implement parcelable
    //
    //
    public miataruFriend(Parcel in) {
        UUID = in.readString();
        Alias = in.readString();
        Latitude = in.readDouble();
        Longitude = in.readDouble();
        Accuracy = in.readFloat();
        TimeStamp = in.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(UUID);
        dest.writeString(Alias);
        dest.writeDouble(Latitude);
        dest.writeDouble(Longitude);
        dest.writeFloat(Accuracy);
        dest.writeLong(TimeStamp);
    }

    public int describeContents () {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public miataruFriend createFromParcel(Parcel in) {
            return new miataruFriend(in);
        }

        public miataruFriend[] newArray(int size) {
            return new miataruFriend[size];
        }
    };


    //
    //
    // getters and putters
    //
    //
    public String getUUID() {
        return UUID;
    }

    public String getAlias() {
        return Alias;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setAlias(String Alias) {
        this.Alias = Alias;
    }

    //
    //
    // Methods to me used with LocationProvider, GoogleMaps and such
    //
    //
    public boolean hasLocation () {
        return hasLocation;
    }

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
