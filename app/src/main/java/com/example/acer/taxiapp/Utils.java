package com.example.acer.taxiapp;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Utils {

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager == null)
            return false;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static boolean isLocationEnabled(Context context) {
        LocationManager lManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gpsEnabled || networkEnabled;
    }

    public static void hideKeyboard(View view) {
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static boolean isFragmentVisible(Fragment fragment) {
        return fragment != null && fragment.isVisible();
    }

    public static int getColor(Context context, int resourceId) {
        return ResourcesCompat.getColor(context.getResources(), resourceId, null);
    }

    public static Drawable getDrawable(Context context, int resourceId) {
        return ResourcesCompat.getDrawable(context.getResources(), resourceId, null);
    }

}
