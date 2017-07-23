package com.example.acer.taxiapp;

import android.location.Location;
import android.os.CountDownTimer;
import android.util.Log;

public class DiscountCalculator {

    private static final double DISCOUNT_PRIZE = 40.0;
    private static final int DISCOUNT_IN_METERS = 1400;
    private static final int DISCOUNT_IN_SECONDS = 0 * 60;
    private static final double PRIZE_PER_METER = DISCOUNT_PRIZE / DISCOUNT_IN_METERS;
    private static final double PRIZE_PER_SECOND = DISCOUNT_PRIZE / DISCOUNT_IN_SECONDS;
    private static final float SPEED_THRESHOLD = 20 * 1000 / 3600;

    private OnDiscountCompleteListener listener;

    private Location lastLocation;
    private long lastTime;

    private double totalDiscount;
    private double distanceInMeters;
    private long elapsedTimeInSeconds;

    public DiscountCalculator(OnDiscountCompleteListener listener) {
        this.listener = listener;
    }

    public void startCalculating(Location location) {
        totalDiscount = 0.0;
        distanceInMeters = 0.0;
        elapsedTimeInSeconds = 0;
        lastLocation = location;
        lastTime = System.currentTimeMillis();
        listener.onDiscountStarted();
//        new CountDownTimer((long) DISCOUNT_IN_SECONDS * 1000, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                add(lastLocation);
//            }
//
//            @Override
//            public void onFinish() {
//
//            }
//        }.start();
    }

    public void add(Location location) {
        long currentTime = System.currentTimeMillis();
        if(location.getSpeed() > SPEED_THRESHOLD) {
            double distance = location.distanceTo(lastLocation);
            distanceInMeters += distance;
            totalDiscount += distance * PRIZE_PER_METER;
            Log.e("Discount", "Meters: " + distanceInMeters + "\nMoney: " + totalDiscount);
        } else {
            long elapsedTime = (currentTime - lastTime) / 1000;
            elapsedTimeInSeconds += elapsedTime;
            totalDiscount += elapsedTime * PRIZE_PER_SECOND;
            Log.e("Discount", "Time: " + elapsedTimeInSeconds + "\nMoney: " + totalDiscount);
        }
        if(totalDiscount >= DISCOUNT_PRIZE) {
            listener.onDiscountComplete(distanceInMeters, elapsedTimeInSeconds);
            finishCalculating();
            return;
        }
        lastLocation = location;
        lastTime = currentTime;
    }

    private void finishCalculating() {
        lastLocation = null;
    }

    public void cancel() {
        finishCalculating();
    }

    public interface OnDiscountCompleteListener {
        void onDiscountStarted();
        void onDiscountComplete(double distanceInMeters, long timeInSeconds);
    }

}
