package com.example.acer.taxiapp;


import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LocationUpdater {

    private static final int INTERVAL = 10;

    private Context context;
    private TCPClient tcpClient;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledFuture;
    private Location lastLocation;

    private volatile boolean isRunning;


    private final Object locationLock = new Object();

    public LocationUpdater(Context context) {
        this.context = context;
        this.tcpClient = TCPClient.getInstance(context);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = false;
    }

    public void start() {
        if(!isRunning) {
            scheduledFuture = scheduler.scheduleWithFixedDelay(new ScheduledUpdateTask(), 10, INTERVAL, TimeUnit.SECONDS);
            isRunning = true;
        }
    }

    public void stop() {
        if(isRunning) {
            scheduler.shutdownNow();
            isRunning = false;
        }
    }

    public void execute(Location location) {

    }

    public void setLastLocation(Location location) {
        synchronized (locationLock) {
            this.lastLocation = location;
        }
    }

    private class ScheduledUpdateTask implements Runnable {
        @Override
        public void run() {
            if(lastLocation != null) {
                byte[] message = MessengerClient.getCommonMessage(lastLocation, context);
                tcpClient.sendBytes(message);

            }
        }
    }
}
