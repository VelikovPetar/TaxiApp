package com.example.acer.taxiapp;


import android.location.Location;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Acer on 18.5.2017.
 */

public class LocationUpdater {

    private static final int INTERVAL = 10;

    private TCPClient tcpClient;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledFuture;
    private Location lastLocation;

    private final Object locationLock = new Object();

    public LocationUpdater(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduledFuture = scheduler.scheduleWithFixedDelay(new ScheduledUpdateTask(), 0, INTERVAL, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
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
//            byte[] message = new byte[64];
//            tcpClient.sendByte(message);
        }
    }
}
