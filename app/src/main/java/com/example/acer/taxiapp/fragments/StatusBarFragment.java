package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.acer.taxiapp.BroadcastActions;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.TCPClient;

/**
 * Created by Acer on 15.5.2017.
 */

public class StatusBarFragment extends Fragment {

    // TextViews
    private TextView driverStatus;
    private TextView vehicleStatus;
    private TextView locationStatus;
    private TextView connectionStatus;
    private TextView serverStatus;

    // Broadcast receiver
    private StatusBarUpdatesBroadcastReceiver receiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_bar, container, false);
        driverStatus = (TextView) view.findViewById(R.id.text_view_driver_status);
        driverStatus.setText("Unknown");
        vehicleStatus = (TextView) view.findViewById(R.id.text_view_vehicle_status);
        vehicleStatus.setText("Unknown");
        locationStatus = (TextView) view.findViewById(R.id.text_view_location_status);
        locationStatus.setText("GPS + WiFi");
        connectionStatus = (TextView) view.findViewById(R.id.text_view_connection_status);
        connectionStatus.setText("NO INFO");
        serverStatus = (TextView) view.findViewById(R.id.text_view_server_status);
        serverStatus.setText("Connected");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        receiver = new StatusBarUpdatesBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_DRIVER_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_LOCATION_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_CONNECTION_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_SERVER_STATUS);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
        Log.e("BROAD", "Receiver registered");
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    private class StatusBarUpdatesBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("BROAD", "Receiver called");
            String action = intent.getAction();
            String value = intent.getStringExtra(TCPClient.VALUE);
            int color = intent.getIntExtra(TCPClient.COLOR, Color.GRAY);
            switch(action) {
                case BroadcastActions.ACTION_DRIVER_STATUS:
                    driverStatus.setText(value);
                    driverStatus.setTextColor(color);
                    Log.e("BROAD", "1");
                    break;
                case BroadcastActions.ACTION_LOCATION_STATUS:
                    locationStatus.setText(value);
                    locationStatus.setTextColor(color);
                    Log.e("BROAD", "2");
                    break;
                case BroadcastActions.ACTION_CONNECTION_STATUS:
                    connectionStatus.setText(value);
                    connectionStatus.setTextColor(color);
                    Log.e("BROAD", "3");
                    break;
                case BroadcastActions.ACTION_SERVER_STATUS:
                    serverStatus.setText(value);
                    serverStatus.setTextColor(color);
                    Log.e("BROAD", "4");
                    break;
            }
            Log.e("BROAD", "End");
        }
    }


    public interface StatusUpdate {
        String getValue();
        int getColor();
    }

    public static class DriverStatusValue implements StatusUpdate {

        private String value;
        private int color;

        public DriverStatusValue(String value, int color) {
            this.value = value;
            this.color = color;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public int getColor() {
            return color;
        }
    }

    public enum LocationStatusValues implements StatusUpdate {
        NO_LOCATION_SERVICE("No location service", Color.RED),
        NETWORK("Network", Color.YELLOW),
        GPS("GPS", Color.GREEN);

        private String value;
        private int color;
        LocationStatusValues(String value, int color) {
            this.value = value;
            this.color = color;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public int getColor() {
            return color;
        }
    }

    public enum ConnectionStatusValues implements StatusUpdate {
        NOT_CONNECTED("No connection", Color.RED),
        CONNECTING("Connecting...", Color.YELLOW),
        CONNECTED("Connected", Color.GREEN);

        private String value;
        private int color;
        ConnectionStatusValues(String value, int color) {
            this.value = value;
            this.color = color;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public int getColor() {
            return color;
        }
    }

    public enum ServerStatusValues implements StatusUpdate {
        NOT_CONNECTED("No connection", Color.RED),
        CONNECTING("Connecting...", Color.YELLOW),
        CONNECTED("Connected", Color.GREEN);

        private String value;
        private int color;
        ServerStatusValues(String value, int color) {
            this.value = value;
            this.color = color;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public int getColor() {
            return color;
        }
    }
}
