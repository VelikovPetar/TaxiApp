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
    private VehicleState state;

    private volatile boolean isRunning;


    private final Object locationLock = new Object();
    private final Object stateLock = new Object();

    public LocationUpdater(Context context) {
        this.context = context;
        this.tcpClient = TCPClient.getInstance(context);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = false;
        this.state = VehicleState.SLOBODEN;
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

    public void setState(VehicleState state) {
        synchronized (stateLock) {
            this.state = state;
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
            // Avoiding nested synchronized blocks
            byte[] message = null;
            Location current = null;
            synchronized (locationLock) {
                current = new Location(lastLocation);
            }
            synchronized (stateLock) {
                message = MessengerClient.getCommonMessage(current, context, state);
            }
            tcpClient.sendBytes(message);
        }
    }
}
