package com.example.topatu;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.CircleOptions;


/**
 * Created by Ixa on 16/02/2015.
 */
public class miataruLocation extends Location {
    private String UUID;
    private String Alias;

    //
    //
    // Different constructors
    //
    //
    public miataruLocation(Location loc) {
        super(loc);
        UUID = null;
        Alias = null;
    }

    public miataruLocation(String UUID) {
        super("Miataru");
        this.UUID = UUID;
    }

    public miataruLocation(String UUID, String Alias) {
        super("Miataru");
        this.UUID = UUID;
        this.Alias = Alias;
    }

    public miataruLocation() {
        super("Miataru");
    }

    //
    //
    // Methods to implement parcelable
    //
    //
    /*
    public miataruLocation(Parcel in) {
        super(in);
        UUID = in.readString();
        Alias = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        supper(dest, flags);
        dest.writeString(UUID);
        dest.writeString(Alias);
    }

    public int describeContents () {
        return supper();
    }
    */


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

    void setUUID(String UUID) {
        this.UUID = UUID;
    }

    void setAlias(String Alias) {
        this.Alias = Alias;
    }

    //
    //
    // Methods to me used with LocationProvider, GoogleMaps and such
    //
    //
    LatLng getLatLng () {
        return new LatLng(this.getLatitude(),this.getLongitude());
    }

    CircleOptions getCircleOptions() {
        CircleOptions options = new CircleOptions();
        options.center(getLatLng());
        options.radius(this.getAccuracy());

        return options;
    }

    MarkerOptions getMarkerOptions () {
        MarkerOptions options = new MarkerOptions();

        options.position(getLatLng());

        options.title(Alias);

        if ( Alias.compareTo(UUID) != 0 ) {
            options.snippet(UUID);
        }

        return options;
    }
}
