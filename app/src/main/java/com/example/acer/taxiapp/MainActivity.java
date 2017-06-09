package com.example.acer.taxiapp;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.acer.taxiapp.fragments.MapFragment;
import com.example.acer.taxiapp.fragments.MessagesFragment;
import com.example.acer.taxiapp.fragments.OffersFragment;
import com.example.acer.taxiapp.fragments.OffersStatusBarFragment;
import com.example.acer.taxiapp.fragments.StatusBarFragment;
import com.example.acer.taxiapp.services.TCPClientService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements LocationListener,
        MessagesFragment.MessageListProvider,
        OffersFragment.ShortOffersListProvider,
        LoginFragment.LoginCallbacks{

    // Constants
    public static final String PREFERENCES = "my_preferences";
    public static final String RF_CARD_ID = "rf_card";
    public static final String DEVICE_ID = "device_id";

    // Debug
    private String DEBUG_TAG = "LONG OFFER";
    private boolean debug = true;

    // Location manager
    private LocationManager locationManager;
    // Last known location
    private Location lastLocation;

    boolean isMapVisible;

    // Thread for automatically sending periodical location updates
    private LocationUpdater locationUpdater;
    // State of the vehicle
    private VehicleState state = VehicleState.SLOBODEN;

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
//    private boolean isBound = false;

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
//    private void doBind() {
//        if(!isBound) {
//            bindService(new Intent(MainActivity.this, TCPClientIntentService.class), serviceConnection, BIND_AUTO_CREATE);
//            isBound = true;
//        }
//    }

//    private void doUnbind() {
//        if(isBound) {
//            unbindService(serviceConnection);
//            isBound = false;
//        }
//    }
//
//    NetworkChangeReceiver receiver;

//    private Location gpsLocation;
//    private Location networkLocation;

    // Handler for delayed actions
    private Handler handler;

    // Indicator whether the driver is logged in
    private boolean isLoggedIn = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Initialize fragments
        final FragmentManager fManager = getFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.add(R.id.fragment_offers_container, new OffersStatusBarFragment(), "TAG_OFFERS_STATUS_BAR_FRAGMENT");
            fTransaction.add(R.id.fragment_buttons_container, new ButtonListFragment(), "TAG_BUTTONS_LIST_FRAGMENT");
            fTransaction.add(R.id.fragment_content_container, new LoginFragment(), "TAG_LOGIN_FRAGMENT");
            fTransaction.add(R.id.fragment_status_bar_container, new StatusBarFragment(), "TAG_STATUS_BAR_FRAGMENT");
            fTransaction.commit();
        }

        // TODO Naming
        // TODO Disable the button when a driver is logged in
        ImageButton configButton = (ImageButton) findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigFragment configFragment = (ConfigFragment) fManager.findFragmentByTag("TAG_CONFIG_FRAGMENT");
                if(configFragment == null || !configFragment.isVisible()) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new ConfigFragment(), "TAG_CONFIG_FRAGMENT");
                    fTransaction.addToBackStack("frag_conf");
                    fTransaction.commit();
                }
            }
        });

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);
        }

        // Initialize the Location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // bind to the Tcp client service
        Log.e(DEBUG_TAG, "BINDING");
        Intent intent = new Intent(this, TCPClientService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        Log.e("LIFECYCLE", "ON CREATE");

        // Init handler
        handler = new Handler();

        // Init location updater
        locationUpdater = new LocationUpdater(this);

        // TODO REMOVE -----------------------------------------------------------------------------
//        final ShortOffer so1 = new ShortOffer(1234, (byte)'0', "Nikola Parapunov 12");
//        shortOffers.add(so1);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (!so1.isCanceled()) {
//                    shortOffers.remove(so1);
//                     If the offers fragment is visible, update the list view displaying the short offers
//                    FragmentManager fManager = getFragmentManager();
//                    OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
//                    if (offersFragment != null && offersFragment.isVisible()) {
//                        offersFragment.notifyDataSetChanged();
//                    }
//
//                     Update the offers status bar
//                    OffersStatusBarFragment offersStatusBarFragment =
//                            (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
//                    if (offersStatusBarFragment != null && offersStatusBarFragment.isVisible()) {
//                        offersStatusBarFragment.setOffersCount(shortOffers.size());
//                    }
//                    Log.e(DEBUG_TAG, "Runnable called");
//                }
//            }
//        }, 10000);
//        ShortOffer so2 = new ShortOffer(1235, (byte)'3', "Bulevar Ilinden 12");
//        shortOffers.add(so2);
//        ShortOffer so3 = new ShortOffer(1236, (byte)'3', "Partizanska 1234");
//        shortOffers.add(so3);
//        shortOffers.add(new ShortOffer(1237, (byte) '3', "Bakalnska"));
//        shortOffers.add(new ShortOffer(1238, (byte) '3', "Goce delchev"));
//        shortOffers.add(new ShortOffer(1239, (byte) '3', "Butel 23"));
//        shortOffers.add(new ShortOffer(12310, (byte) '3', "mavrovka 12"));
//        shortOffers.add(new ShortOffer(12311, (byte) '3', "Ulica makedonija"));
//        shortOffers.add(new ShortOffer(12312, (byte) '3', "Rekord 12"));
//        shortOffers.add(new ShortOffer(12313, (byte) '3', "Shutka 1234"));
//        shortOffers.add(new ShortOffer(12314, (byte) '3', "City Mall"));
//        shortOffers.add(new ShortOffer(12315, (byte) '3', "Arhiv"));
//        shortOffers.add(new ShortOffer(12316, (byte) '3', "Zhdanec"));
//        shortOffers.add(new ShortOffer(12317, (byte) '3', "Porta Vlae"));
        // TODO Remove -----------------------------------------------------------------------------
    }

    public void showLoginFragment(View v) {
        FragmentManager fManager = getFragmentManager();
        boolean isPopped = fManager.popBackStackImmediate("frag_conf", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if(!isPopped) {
            Log.e("POPPING", "Not in stack");
            LoginFragment loginFragment = (LoginFragment) fManager.findFragmentByTag("TAG_LOGIN_FRAGMENT");
            if(loginFragment == null || !loginFragment.isVisible()) {
                FragmentTransaction fTransaction = fManager.beginTransaction();
                fTransaction.replace(R.id.fragment_content_container, new LoginFragment(), "TAG_LOGIN_FRAGMENT");
                fTransaction.addToBackStack(null);
                fTransaction.commit();
            }
        } else {
            Log.e("POPPING", "IN stack");
        }
    }

    public void sendLoginMessage(View view) {
        if(lastLocation != null) {
            byte[] message = MessengerClient.getLoginMessage(lastLocation, this);
            tcpClientService.sendBytes(message);
        }
    }

    public void sendLogoutMessage(View view) {
        if(lastLocation != null) {
            byte[] message = MessengerClient.getLogoutMessage(lastLocation, this);
            tcpClientService.sendBytes(message);
        }
    }

    public void sendPauseStartMessage(View view) {
        if(lastLocation != null) {
            byte[] message = MessengerClient.getPauseStartMessage(lastLocation, this);
            tcpClientService.sendBytes(message);
        }
    }

    public void sendPauseStopMessage(View view) {
        if(lastLocation != null) {
            byte[] message = MessengerClient.getPauseStopMessage(lastLocation, this);
            tcpClientService.sendBytes(message);
        }
    }

    public void showMessagesFragment(View view) {
        FragmentManager fManager = getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        MessagesFragment messagesFragment = new MessagesFragment();
        fTransaction.replace(R.id.fragment_content_container, messagesFragment, "TAG_POPUP_MESSAGES_FRAGMENT");
        fTransaction.commit();
    }

    public void showOffersFragment(View view) {
        FragmentManager fManager = getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        OffersFragment messagesFragment = new OffersFragment();
        fTransaction.replace(R.id.fragment_content_container, messagesFragment, "TAG_OFFERS_FRAGMENT");
        fTransaction.commit();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.e("LIFECYCLE", "ON START");

        // Register receiver for status bar updates
//        IntentFilter intentFilter3 = new IntentFilter();
//        intentFilter3.addAction(BroadcastActions.ACTION_DRIVER_STATUS);
//        intentFilter3.addAction(BroadcastActions.ACTION_VEHICLE_STATE_STATUS);
//        intentFilter3.addAction(BroadcastActions.ACTION_LOCATION_STATUS);
//        intentFilter3.addAction(BroadcastActions.ACTION_CONNECTION_STATUS);
//        intentFilter3.addAction(BroadcastActions.ACTION_SERVER_STATUS);
//        LocalBroadcastManager.getInstance(this).registerReceiver(statusBarUpdatesBroadcastReceiver, intentFilter3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO Konsultiraj se shto da pravi aplikacijata ako e vo pozadina!

        // Register receiver for status bar updates
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(BroadcastActions.ACTION_DRIVER_STATUS);
        intentFilter3.addAction(BroadcastActions.ACTION_VEHICLE_STATE_STATUS);
        intentFilter3.addAction(BroadcastActions.ACTION_LOCATION_STATUS);
        intentFilter3.addAction(BroadcastActions.ACTION_CONNECTION_STATUS);
        intentFilter3.addAction(BroadcastActions.ACTION_SERVER_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusBarUpdatesBroadcastReceiver, intentFilter3);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
            lastLocation = getLastKnownLocation();
            updateStatusBarLocation();

            // Start the thread that sends periodical location updates
            if(isLoggedIn && !locationUpdater.isRunning()) {
                locationUpdater.setLastLocation(lastLocation);
                locationUpdater.start();
            }
        }

        // Register receiver for popup messages
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_POPUP_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(popupMessageReceiver, intentFilter);

        // Register receiver for short offers
        offersReceiver = new OffersReceiver(handler);
        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction(BroadcastActions.ACTION_SHORT_OFFER);
        intentFilter1.addAction(BroadcastActions.ACTION_CANCEL_SHORT_OFFER);
        intentFilter1.addAction(BroadcastActions.ACTION_LONG_OFFER);
        LocalBroadcastManager.getInstance(this).registerReceiver(offersReceiver, intentFilter1);

        // Register receiver for vehicle state updates
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(BroadcastActions.ACTION_VEHICLE_STATE_FOR_LOCATION_UPDATES);
        LocalBroadcastManager.getInstance(this).registerReceiver(vehicleStateReceiver, intentFilter2);

        //  TODO REMOVE -----------------------------------------------------------------------------
//        ((OffersStatusBarFragment)getFragmentManager().findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT")).setOffersCount(shortOffers.size());
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Parser parser = new Parser(MainActivity.this);
//                parser.broadcastLongOffer(12345, 42.00653f, 21.382f, (byte)'1', "Nikola Parapunov 27");
//            }
//        }, 5000);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 7500);
        // TODO REMOVE -----------------------------------------------------------------------------

        Log.e("LIFECYCLE", "ON RESUME");
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

        // Stop the thread sending periodical location updates
        if(isLoggedIn && locationUpdater.isRunning()) {
            locationUpdater.stop();
        }
        locationManager.removeUpdates(this);

        // Unregister popup message receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(popupMessageReceiver);

        // Unregister short offers receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(offersReceiver);

        // Unregister vehicle state receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vehicleStateReceiver);

        // Unregister status bar updates receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusBarUpdatesBroadcastReceiver);

        Log.e("LIFECYCLE", "ON PAUSE");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.e("LIFECYCLE", "ON STOP");
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusBarUpdatesBroadcastReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLoggedIn) {
            TCPClient client = TCPClient.getInstance(this);
            client.sendBytes(MessengerClient.getLogoutMessage(lastLocation, this));
            isLoggedIn = false;
        }
        unbindService(serviceConnection);
        Log.e("LIFECYCLE", "ON DESTROY");
    }

    @Override
    public void onBackPressed() {
        if(getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1234 && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "PLEASE GIVE PERMISSION TO USE FINE LOCATION", Toast.LENGTH_SHORT).show();
            } else {
                updateStatusBarLocation();
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
        Toast.makeText(this, lastLocation.getLatitude() + " " + lastLocation.getLongitude() + " from " + lastLocation.getProvider(), Toast.LENGTH_SHORT).show();
        // TODO Replace with boolean isMapVisible
        FragmentManager fManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) fManager.findFragmentByTag("TAG_MAP_FRAGMENT");
        if(mapFragment != null && mapFragment.isVisible()) {
            mapFragment.updateLocation((float) lastLocation.getLatitude(), (float)lastLocation.getLongitude());
        }
    }

    // Displaying the changes in status of the location services
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
                        if (networkAvailable) {
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
                        if (!gpsAvailable) {
                            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                    StatusBarFragment.LocationStatusValues.NETWORK);
                        }
                        networkAvailable = true;
                        break;
                    default:
                        if (!gpsAvailable) {
                            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS,
                                    StatusBarFragment.LocationStatusValues.NO_LOCATION_SERVICE);
                        }
                        networkAvailable = false;
                        break;
                }
                break;
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        updateStatusBarLocation();
    }

    @Override
    public void onProviderDisabled(String provider) {
        updateStatusBarLocation();
    }

    private void updateStatusBarLocation() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            // Show no location
            gpsAvailable = networkAvailable = false;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.NO_LOCATION_SERVICE);
        } else if (!gpsEnabled) {
            // Show network
            gpsAvailable = false;
            networkAvailable = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.NETWORK);
        } else if (!networkEnabled) {
            // Show gps
            gpsAvailable = true;
            networkAvailable = false;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.GPS);
        } else {
            gpsAvailable = networkAvailable = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_LOCATION_STATUS, StatusBarFragment.LocationStatusValues.GPS);
        }
    }

    // helper function that gets the best last known location
    private Location getLastKnownLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Location tmp = locationManager.getLastKnownLocation(provider);
            if(tmp == null)
                continue;
            if(bestLocation == null || bestLocation.getAccuracy() > tmp.getAccuracy()) {
                bestLocation = tmp;
            }
        }
        return bestLocation;
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
    }

    // Listener for changes in network connection
//    private class NetworkChangeReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent _intent) {
//            Log.e(DEBUG_TAG, "IN LOCAL ON RECEIVED");
//            if(Utils.hasInternetConnection(context)) {
//                Intent intent = new Intent(context, TCPClientIntentService.class);
//                startService(intent);
//                doBind();
//                Log.e(DEBUG_TAG, "Started service");
//                Intent broadcast = new Intent();
//                broadcast.setAction(BroadcastActions.ACTION_CONNECTION_STATUS);
//                broadcast.putExtra(TCPClientIntentService.VALUE, "Connected");
//                broadcast.putExtra(TCPClientIntentService.COLOR, Color.GREEN);
//                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
//                Log.e(DEBUG_TAG, "Broadcast sent");
//
//            } else {
//                doUnbind();
//                Intent intent = new Intent(context, TCPClientIntentService.class);
//                stopService(intent);
//                Log.e(DEBUG_TAG, "Stopped service");
//                Intent broadcast = new Intent();
//                broadcast.setAction(BroadcastActions.ACTION_CONNECTION_STATUS);
//                broadcast.putExtra(TCPClientIntentService.VALUE, "No connection");
//                broadcast.putExtra(TCPClientIntentService.COLOR, Color.RED);
//                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
//                Log.e(DEBUG_TAG, "Sent broadcast");
//            }
//        }
//    }



    // List that keeps most recent popup messages
    private FixedSizeList<String> popupMessages = new FixedSizeList<>(10);

    // Receiver instance
    private PopupMessageReceiver popupMessageReceiver = new PopupMessageReceiver();

    @Override
    public List<String> getMessages() {
        return popupMessages.getElements();
    }

    // Broadcast receiver for incoming popup messages
    private class PopupMessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_POPUP_MESSAGE)) {
                // Add the latest message to the list
                String message = intent.getStringExtra(Parser.MESSAGE);
                popupMessages.insert(message);

                // If the message fragment is visible, update the list view displaying the messages
                FragmentManager fManager = getFragmentManager();
                MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
                if(messagesFragment != null && messagesFragment.isVisible()) {
                    messagesFragment.notifyDataSetChanged();
                }

                // Update the offers status bar
                OffersStatusBarFragment offersStatusBarFragment =
                        (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
                if(offersStatusBarFragment != null && offersStatusBarFragment.isVisible()) {
                    offersStatusBarFragment.setMessagesCount(popupMessages.size());
                }
            }
        }
    }



    // List that keeps current short offers
    private ArrayList<ShortOffer> shortOffers = new ArrayList<>();

    // The active long offer
    private LongOffer longOffer;

    // Receiver instance
    private OffersReceiver offersReceiver = new OffersReceiver(handler);

    @Override
    public List<ShortOffer> getShortOffers() {
        return shortOffers;
    }

    @Override
    public LongOffer getLongOffer() {
        return longOffer;
    }

    // Broadcast receiver for incoming short offers
    private class OffersReceiver extends BroadcastReceiver {

        private Handler receiverHandler;

        public OffersReceiver(Handler handler) {
            this.receiverHandler = handler;
        }

        private void updateFragments(LongOffer longOffer) {
            FragmentManager fManager = getFragmentManager();
            // If the offers fragment is visible, update the list view displaying the short offers
            OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
            if(offersFragment != null && offersFragment.isVisible()) {
                offersFragment.notifyDataSetChanged();
                if(longOffer != null) {
                    offersFragment.displayLongOffer(longOffer);
                }
            }
            // Update the offers status bar
            OffersStatusBarFragment offersStatusBarFragment =
                    (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
            if(offersStatusBarFragment != null && offersStatusBarFragment.isVisible()) {
                offersStatusBarFragment.setOffersCount(shortOffers.size());
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Deaktiviraj ja aktivnata dolga najava
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_SHORT_OFFER)) {
                long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                byte offerSource = intent.getByteExtra(Parser.OFFER_SOURCE, (byte) -1);
                String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                final ShortOffer shortOffer = new ShortOffer(idPhoneCall, offerSource, textMessage);
                shortOffers.add(0, shortOffer);
                receiverHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!shortOffer.isCanceled()) {
                            shortOffers.remove(shortOffer);
                            updateFragments(null);
                        }
                    }
                }, 45000);
                updateFragments(null);
            } else if(action.equals(BroadcastActions.ACTION_CANCEL_SHORT_OFFER)) {
                long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                for(ShortOffer so : shortOffers) {
                    if(so.getIdPhoneCall() == idPhoneCall) {
                        so.cancel(textMessage);
                        updateFragments(null);
                        break;
                    }
                }
            } else if(action.equals(BroadcastActions.ACTION_LONG_OFFER)) {
                long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                float latitude = intent.getFloatExtra(Parser.LATITUDE, -1);
                float longitude = intent.getFloatExtra(Parser.LONGITUDE, -1);
                byte offerSource = intent.getByteExtra(Parser.OFFER_SOURCE, (byte) -1);
                String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                longOffer = new LongOffer(idPhoneCall, latitude, longitude, offerSource, textMessage);

                // Remove all short offers and callbacks scheduled on the handler
                receiverHandler.removeCallbacksAndMessages(null);
                shortOffers.clear();
                // Display the long offer
                updateFragments(longOffer);
            }
        }
    }


    // Receiver instance
    private VehicleStateReceiver vehicleStateReceiver = new VehicleStateReceiver();

    // Broadcast receiver for vehicle state updates
    // Used for generating the correct message for periodical location updates
    private class VehicleStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_VEHICLE_STATE_FOR_LOCATION_UPDATES)) {
                int stateId = intent.getIntExtra(Parser.VEHICLE_STATE, -1);
                if(stateId != -1) {
                    VehicleState state = VehicleState.getByValue(stateId);
                    locationUpdater.setState(state);
                }
            }
        }
    }



    // Receiver instance
    private StatusBarUpdatesBroadcastReceiver statusBarUpdatesBroadcastReceiver = new StatusBarUpdatesBroadcastReceiver();

    // Broadcast receiver for status bar updates
    private class StatusBarUpdatesBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String value = intent.getStringExtra(TCPClient.VALUE);
            int color = intent.getIntExtra(TCPClient.COLOR, Color.GRAY);
            StatusBarFragment statusBarFragment = (StatusBarFragment) getFragmentManager().findFragmentByTag("TAG_STATUS_BAR_FRAGMENT");
            statusBarFragment.update(action, value, color);
        }
    }


    @Override
    public void onSuccessfulLogin() {
        isLoggedIn = true;
        isMapVisible = true;
        FragmentManager fManager = getFragmentManager();
        fManager.popBackStackImmediate();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        fTransaction.replace(R.id.fragment_content_container, new MapFragment(), "TAG_MAP_FRAGMENT");
        fTransaction.commit();
        TCPClient tcpClient = TCPClient.getInstance(this);
        tcpClient.sendBytes(MessengerClient.getLoginMessage(lastLocation, this));

        // Start the thread that sends periodical location updates
        locationUpdater.setLastLocation(lastLocation);
        locationUpdater.start();
    }
}
