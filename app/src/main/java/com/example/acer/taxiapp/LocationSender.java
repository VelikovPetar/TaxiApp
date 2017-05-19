package com.example.acer.taxiapp;

import android.location.Location;

/**
 * Created by Acer on 18.5.2017.
 */

public abstract class LocationSender {
    public abstract void sendUpdate(Location location);
}
