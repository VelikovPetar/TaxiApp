package com.example.acer.taxiapp;

import android.location.Location;

public abstract class LocationSender {
    public abstract void sendUpdate(Location location);
}
