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

public class StatusBarFragment extends Fragment {

    // TextViews
    private TextView driverStatus;
    private TextView vehicleStatus;
    private TextView locationStatus;
    private TextView connectionStatus;
    private TextView serverStatus;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_bar, container, false);
        driverStatus = (TextView) view.findViewById(R.id.text_view_driver_status);
        driverStatus.setText("Unknown");
        vehicleStatus = (TextView) view.findViewById(R.id.text_view_vehicle_status);
        vehicleStatus.setText("Unknown");
        locationStatus = (TextView) view.findViewById(R.id.text_view_location_status);
        locationStatus.setText("NO INFO");
        connectionStatus = (TextView) view.findViewById(R.id.text_view_connection_status);
        connectionStatus.setText("NO INFO");
        serverStatus = (TextView) view.findViewById(R.id.text_view_server_status);
        serverStatus.setText("NO INFO");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void update(String action, String value, int color) {
        switch(action) {
            case BroadcastActions.ACTION_DRIVER_STATUS:
                driverStatus.setText(value);
                driverStatus.setTextColor(color);
                break;
            case BroadcastActions.ACTION_VEHICLE_STATE_STATUS:
                vehicleStatus.setText(value);
                vehicleStatus.setTextColor(color);
                break;
            case BroadcastActions.ACTION_LOCATION_STATUS:
                locationStatus.setText(value);
                locationStatus.setTextColor(color);
                break;
            case BroadcastActions.ACTION_CONNECTION_STATUS:
                connectionStatus.setText(value);
                connectionStatus.setTextColor(color);
                break;
            case BroadcastActions.ACTION_SERVER_STATUS:
                serverStatus.setText(value);
                serverStatus.setTextColor(color);
                break;
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

    public static class VehicleStatusValue implements StatusUpdate {
        private String value;
        private int color;

        public VehicleStatusValue(String value, int color) {
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
