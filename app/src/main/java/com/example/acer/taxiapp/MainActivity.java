package com.example.acer.taxiapp;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.acer.taxiapp.fragments.ButtonListFragment;
import com.example.acer.taxiapp.fragments.ConfigFragment;
import com.example.acer.taxiapp.fragments.LoginFragment;
import com.example.acer.taxiapp.fragments.OffersFragment;
import com.example.acer.taxiapp.fragments.StatusBarFragment;
import com.example.acer.taxiapp.services.TCPClientIntentService;
import com.example.acer.taxiapp.services.TCPClientService;


public class MainActivity extends Activity implements LocationListener {


    // Constants
    public static final String PREFERENCES = "my_preferences";
    public static final String RF_CARD_ID = "rf_card";
    public static final String DEVICE_ID = "device_id";
    // Debug
    private String DEBUG_TAG = "TCP";
    private boolean debug = true;

    LocationManager locationManager;
    Location lastLocation;
    boolean isMapVisible;

    // Communicating with the TcpClientService
    private TCPClientService tcpClientService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TCPClientService.MyBinder binder = (TCPClientService.MyBinder) service;
            tcpClientService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            tcpClientService = null;
        }
    };
    private boolean isBound = false;

    // Communicating with the TcpClientIntentService
//    private TCPClientIntentService tcpClientIntentService;
//    private ServiceConnection serviceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            tcpClientIntentService = ((TCPClientIntentService.TCPServiceBinder) service).getService();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            tcpClientIntentService = null;
//        }
//    };
//    private boolean isBound = false;
//
    private void doBind() {
        if(!isBound) {
            bindService(new Intent(MainActivity.this, TCPClientIntentService.class), serviceConnection, BIND_AUTO_CREATE);
            isBound = true;
        }
    }

    private void doUnbind() {
        if(isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
//
//    NetworkChangeReceiver receiver;
    private LocationUpdater locationUpdater;
    private Location gpsLocation;
    private Location networkLocation;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        final FragmentManager fManager = getFragmentManager();
        if(savedInstanceState == null) {
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.add(R.id.fragment_offers_container, new OffersFragment());
            fTransaction.add(R.id.fragment_buttons_container, new ButtonListFragment());
            fTransaction.add(R.id.fragment_content_container, new LoginFragment());
            fTransaction.add(R.id.fragment_status_bar_container, new StatusBarFragment());
            fTransaction.commit();
        }

        ImageButton configButton = (ImageButton) findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fTransaction = fManager.beginTransaction();
                fTransaction.replace(R.id.fragment_content_container, new ConfigFragment());
                fTransaction.commit();
            }
        });


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);
        }


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Log.e(DEBUG_TAG, "BINDING");
        Intent intent = new Intent(this, TCPClientService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    public void showLoginFragment(View v) {
        FragmentManager fManager = getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        fTransaction.replace(R.id.fragment_content_container, new LoginFragment());
        fTransaction.commit();
    }

    public void sendLoginMessage(View view) {
        byte[] message = MessengerClient.getLoginMessage(lastLocation, this);
        tcpClientService.sendBytes(message);
    }

    public void sendLogoutMessage(View view) {
        byte[] message = MessengerClient.getLogoutMessage(lastLocation, this);
        tcpClientService.sendBytes(message);
    }

    public void sendPauseStartMessage(View view) {
//        byte[] message = MessengerClient.getCommonMessage(lastLocation, this);
//        tcpClientService.sendBytes(message);
    }

    public void sendPauseStopMessage(View view) {
//        byte[] message = MessengerClient.getPauseStopMessage(lastLocation, this);
//        tcpClientService.sendBytes(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
            lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            updateStatusBarLocation();
            //TODO Fix lock
            locationUpdater = new LocationUpdater(this);
            locationUpdater.setLastLocation(lastLocation);
            locationUpdater.start();
        }


        // Register network status receiver
//        receiver = new NetworkChangeReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister network status receiver
//        unregisterReceiver(receiver);
        locationUpdater.stop();
        locationManager.removeUpdates(this);
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1234 && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1]  != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "PLEASE GIVE PERMISSION TO USE FINE LOCATION", Toast.LENGTH_SHORT).show();
            } else {
                updateStatusBarLocation();
            }
        }
    }


    // TODO: HANDLE DISRUPTIONS IN LOCATION SERVICES IN MAIN ACTIVITY
    // TODO: HANDLE SENDING UPDATES IN SCHEDULED EXECUTOR SERVICE
    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
        Toast.makeText(this, lastLocation.getLatitude() + " " + lastLocation.getLongitude() + " from " + lastLocation.getProvider(), Toast.LENGTH_SHORT).show();

    }


    private boolean networkAvailable, gpsAvailable;
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e("LOCATION", "STATUS of " + provider + " CHANGED");
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
               switch (status) {
                   case LocationProvider.AVAILABLE:
                       broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                               StatusBarFragment.LocationStatusValues.GPS);
                       gpsAvailable = true;
                       break;
                   default:
                       if(networkAvailable) {
                           broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                   StatusBarFragment.LocationStatusValues.NETWORK);
                       } else {
                           broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                   StatusBarFragment.LocationStatusValues.NO_LOCATION_SERVICE);
                       }
                       gpsAvailable = false;
                       break;
               }
               break;
            case LocationManager.NETWORK_PROVIDER:
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        if(!gpsAvailable) {
                            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                    StatusBarFragment.LocationStatusValues.NETWORK);
                        }
                        networkAvailable = true;
                        break;
                    default:
                        if(!gpsAvailable) {
                            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                    StatusBarFragment.LocationStatusValues.NO_LOCATION_SERVICE);
                        }
                        networkAvailable = false;
                        break;
                }
        }

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e("LOCATION", "EN");
        updateStatusBarLocation();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e("LOCATION", "DIS");
        updateStatusBarLocation();
    }

    private void updateStatusBarLocation() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!gpsEnabled && !networkEnabled) {
            // Show no location
            gpsAvailable = networkAvailable = false;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.NO_LOCATION_SERVICE);
        } else if(!gpsEnabled) {
            // Show network
            gpsAvailable = false;
            networkAvailable = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.NETWORK);
        } else if (!networkEnabled){
            // Show gps
            gpsAvailable = true;
            networkAvailable = false;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.GPS);
        } else {
            gpsAvailable = networkAvailable = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.GPS);
        }
    }

    // Name for broadcasts concerning status bar updates
    public static final String VALUE = "status_bar_update_value";
    public static final String COLOR = "status_bar_update_color";

    private void broadcastStatusUpdate(String action, StatusBarFragment.StatusUpdate statusUpdate) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(VALUE, statusUpdate.getValue());
        intent.putExtra(COLOR, statusUpdate.getColor());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.e("BROAD", "CAST");
    }

    // Listener for changes in network connection
    private class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent _intent) {
            Log.e(DEBUG_TAG, "IN LOCAL ON RECEIVED");
            if(Utils.hasInternetConnection(context)) {
                Intent intent = new Intent(context, TCPClientIntentService.class);
                startService(intent);
                doBind();
                Log.e(DEBUG_TAG, "Started service");
                Intent broadcast = new Intent();
                broadcast.setAction(BroadcastActions.ACTION_CONNECTION_STATUS);
                broadcast.putExtra(TCPClientIntentService.VALUE, "Connected");
                broadcast.putExtra(TCPClientIntentService.COLOR, Color.GREEN);
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
                Log.e(DEBUG_TAG, "Broadcast sent");

            } else {
                doUnbind();
                Intent intent = new Intent(context, TCPClientIntentService.class);
                stopService(intent);
                Log.e(DEBUG_TAG, "Stopped service");
                Intent broadcast = new Intent();
                broadcast.setAction(BroadcastActions.ACTION_CONNECTION_STATUS);
                broadcast.putExtra(TCPClientIntentService.VALUE, "No connection");
                broadcast.putExtra(TCPClientIntentService.COLOR, Color.RED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
                Log.e(DEBUG_TAG, "Sent broadcast");
            }
        }
    }

}
