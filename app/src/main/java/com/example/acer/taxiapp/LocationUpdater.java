package com.example.acer.taxiapp;


import android.content.Context;
import android.location.Location;

import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.tcp.TCPClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LocationUpdater {

    private static final int INTERVAL = 15;

    private Context context;
    private TCPClient tcpClient;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledFuture;
    private Location lastLocation;
    private boolean pause = false, taximeter = false, sensor9 = false, sensor10 = false;

    private volatile boolean isRunning;


    private final Object locationLock = new Object();
    private final Object bitsLock = new Object();

    public LocationUpdater(Context context) {
        this.context = context;
        this.tcpClient = TCPClient.getInstance(context);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = false;
    }

    public void start() {
        if(!isRunning) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduledFuture = scheduler.scheduleWithFixedDelay(new ScheduledUpdateTask(), 5, INTERVAL, TimeUnit.SECONDS);
            isRunning = true;
        }
    }

    public void stop() {
        if(isRunning) {
            scheduler.shutdownNow();
            isRunning = false;
        }
    }

    public void setBits(boolean pause, boolean taximeter, boolean sensor9, boolean sensor10) {
        synchronized (bitsLock) {
            this.pause = pause;
            this.taximeter = taximeter;
            this.sensor9 = sensor9;
            this.sensor10 = sensor10;
        }
    }

    public void setBits(boolean pause, boolean taximeter) {
        synchronized (bitsLock) {
            this.pause = pause;
            this.taximeter = taximeter;
        }
    }

    public boolean isRunning() {
        return isRunning;
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
                if(lastLocation == null)
                    return;
                current = new Location(lastLocation);
            }
            synchronized (bitsLock) {
                message = MessengerClient.getCommonMessage(current, context, pause, taximeter, sensor9, sensor10);
            }
            tcpClient.sendBytes(message);
        }
    }
}
