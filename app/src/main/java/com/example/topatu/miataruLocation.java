package com.example.topatu;

import android.location.Location;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.CircleOptions;


/**
 * Created by Ixa on 16/02/2015.
 */
public class miataruLocation extends Location {
    private String UUID;
    private String Alias;

    public miataruLocation(Location loc) {
        super(loc);
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

        if ( Alias != null && Alias.length() > 0 ) {
            options.title(Alias);
            options.snippet(UUID);
        } else {
            options.title(UUID);
        }

        return options;
    }
}
