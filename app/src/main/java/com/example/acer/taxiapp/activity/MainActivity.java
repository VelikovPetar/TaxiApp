package com.example.acer.taxiapp.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.DiscountCalculator;
import com.example.acer.taxiapp.LocationUpdater;
import com.example.acer.taxiapp.LoginCallbacks;
import com.example.acer.taxiapp.MessageListProvider;
import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.SoundManager;
import com.example.acer.taxiapp.VehicleState;
import com.example.acer.taxiapp.fragments.LoginFragment;
import com.example.acer.taxiapp.fragments.MapFragment;
import com.example.acer.taxiapp.fragments.OffersFragment;
import com.example.acer.taxiapp.fragments.OffersStatusBarFragment;
import com.example.acer.taxiapp.fragments.StatusBarFragment;
import com.example.acer.taxiapp.models.LongOffer;
import com.example.acer.taxiapp.models.PopupMessage;
import com.example.acer.taxiapp.models.ShortOffer;
import com.example.acer.taxiapp.tcp.Parser;
import com.example.acer.taxiapp.tcp.TCPClient;
import com.example.acer.taxiapp.tcp.TCPClientService;
import com.example.acer.taxiapp.utils.AlertDialogBuilder;
import com.example.acer.taxiapp.utils.BroadcastActions;
import com.example.acer.taxiapp.utils.FixedSizeList;
import com.example.acer.taxiapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends NavigationActivity implements LocationListener,
        MessageListProvider,
        OffersFragment.ShortOffersListProvider,
        LoginFragment.DriverIdProvider,
        LoginCallbacks,
        DiscountCalculator.OnDiscountCompleteListener {

    // Constants
    public static final String PREFERENCES = "my_preferences";
    public static final String DEVICE_ID = "device_id";

    // Location manager
    private LocationManager locationManager;
    // Last known location
    private Location lastLocation;
    // Was received location update for first time
    // Prevents sending messages without ever having read location
    private boolean hasInitialLocation = false;

    // Thread for automatically sending periodical location updates
    private LocationUpdater locationUpdater;

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

    // Handler for delayed actions
    private Handler handler;

    // Id of the logged driver
    private String driverID;
    // Indicator whether the driver is in pause
    private boolean isPaused = false;
    // Indicator whether there is a client
    private boolean isWithClient = false;
    // Indicator if there is a client with discount
    private boolean isWithDiscountClient = false;
    private DiscountCalculator discountCalculator;

    private SoundManager soundManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);
        }

        // Initialize the Location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Init handler
        handler = new Handler();

        // Init location updater
        locationUpdater = new LocationUpdater(this);

        // Init sound pool
        soundManager = new SoundManager(this);

        // Bind to the Tcp client service
        Intent intent = new Intent(this, TCPClientService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onLoginButtonClick(View v) {
        if(!isLoggedIn) {
             super.onLoginButtonClick(lastLocation);
        } else {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.confirm_logout)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (lastLocation != null) {
                                byte[] message = MessengerClient.getLogoutMessage(lastLocation, driverID, MainActivity.this);
                                if (!tcpClientService.sendBytes(message)) {
                                    Toast.makeText(MainActivity.this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
            dialog.show();
        }

    }

    public void onMapButtonClick(View v) {
         super.onMapButtonClick();
    }

    public void onPauseButtonClick(View view) {
        if (!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isPaused) {
            // Send message for starting pause
            byte[] message = MessengerClient.getPauseStartMessage(lastLocation, this);
            if (tcpClientService.sendBytes(message)) {
                locationUpdater.setBits(true, false, false, false);
                isPaused = true;
                // Change button
                Button pauseButton = (Button) findViewById(R.id.button_menu_pause_status);
                pauseButton.setText(R.string.pause_end);
            } else {
                Toast.makeText(this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
        } else {
            // Send message for ending pause
            byte[] message = MessengerClient.getPauseStopMessage(lastLocation, this);
            if (tcpClientService.sendBytes(message)) {
                locationUpdater.setBits(false, false, false, false);
                isPaused = false;
                // Change button
                Button pauseButton = (Button) findViewById(R.id.button_menu_pause_status);
                pauseButton.setText(R.string.pause);
            } else {
                Toast.makeText(this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onClientButtonClick(View view) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        if(!isWithClient) {
            // Send message that the client was picked up
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.discount)
                    .setMessage(R.string.confirm_discount)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            discountCalculator = new DiscountCalculator(MainActivity.this);
                            discountCalculator.startCalculating(lastLocation);
                            if(tcpClientService.sendBytes(MessengerClient.getCommonMessage(lastLocation, MainActivity.this, false, false, true, false))) {
                                locationUpdater.setBits(false, false, true, false);
                                onLongOfferFinished();
                                isWithClient = true;
                                // Change button
                                Button clientStatusButton = (Button) findViewById(R.id.button_menu_client_status);
                                clientStatusButton.setText(R.string.client_dropped_off);
                            } else {
                                Toast.makeText(MainActivity.this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(tcpClientService.sendBytes(MessengerClient.getCommonMessage(lastLocation, MainActivity.this, false, true, false, false))) {
                                locationUpdater.setBits(false, true, false, false);
                                onLongOfferFinished();
                                isWithClient = true;
                                // Change button
                                Button clientStatusButton = (Button) findViewById(R.id.button_menu_client_status);
                                clientStatusButton.setText(R.string.client_dropped_off);
                            } else {
                                Toast.makeText(MainActivity.this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .create();
            dialog.show();
        } else {
            // Send message that client was dropped
            if(tcpClientService.sendBytes(MessengerClient.getCommonMessage(lastLocation, this, false, false))) {
                locationUpdater.setBits(false, false);
                isWithClient = false;
                isWithDiscountClient = false;
                if(discountCalculator != null) {
                    discountCalculator.cancel();
                }
                discountCalculator = null;
                // Change button
                Button clientStatusButton = (Button) findViewById(R.id.button_menu_client_status);
                clientStatusButton.setText(R.string.client_taken);
            } else {
                Toast.makeText(this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void requestStatus(View view) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog dialog = AlertDialogBuilder.build(this, AlertDialogBuilder.STATUS_REQUEST_ALERT_DIALOG);
        dialog.show();
    }

    public void getInfoByRegion(View view) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        byte[] message = MessengerClient.getInfoByRegionMessage(this);
        if(!tcpClientService.sendBytes(message)) {
            Toast.makeText(MainActivity.this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
        }

    }

    public void registerForRegion(View view) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog dialog = AlertDialogBuilder.build(this, AlertDialogBuilder.REGISTER_FOR_REGION_ALERT_DIALOG);
        dialog.show();
    }

    public void showMessagesFragment(View view) {
         super.showMessagesFragment(popupMessages.size());
    }

    public void showOffersFragment(View view) {
         super.showOffersFragment(longOffer, shortOffers);
    }

    public void showGeneratedMessagesFragment(View view) {
         super.showGeneratedMessagesFragment();
    }

    public void showCheckCardFragment(View view) {
         super.showCheckCardFragment(lastLocation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register receiver for status bar updates
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_DRIVER_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_VEHICLE_STATE_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_LOCATION_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_CONNECTION_STATUS);
        intentFilter.addAction(BroadcastActions.ACTION_SERVER_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusBarUpdatesReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver for driver login status
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_DRIVER_LOGIN_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(loginStatusReceiver, intentFilter);

        // Register receiver for popup messages
        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_POPUP_MESSAGE);
        intentFilter.addAction(BroadcastActions.ACTION_INVALID_CARD);
        LocalBroadcastManager.getInstance(this).registerReceiver(popupMessageReceiver, intentFilter);

        // Register receiver for short offers
        offersReceiver = new OffersReceiver(handler);
        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_SHORT_OFFER);
        intentFilter.addAction(BroadcastActions.ACTION_CANCEL_SHORT_OFFER);
        intentFilter.addAction(BroadcastActions.ACTION_LONG_OFFER);
        LocalBroadcastManager.getInstance(this).registerReceiver(offersReceiver, intentFilter);

        // Register receiver for vehicle state updates
        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.ACTION_VEHICLE_STATE_FOR_TIMERS);
        LocalBroadcastManager.getInstance(this).registerReceiver(vehicleStateReceiver, intentFilter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
            lastLocation = getLastKnownLocation();
            updateStatusBarLocation();

            if(!hasInitialLocation) {
                LoginFragment loginFragment = (LoginFragment) getFragmentManager().findFragmentByTag("TAG_LOGIN_FRAGMENT");
                if (loginFragment != null && lastLocation != null) {
                    loginFragment.initLocation(lastLocation);
                    hasInitialLocation = true;
                }
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        // Unregister login status receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginStatusReceiver);
        // Unregister popup message receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(popupMessageReceiver);
        // Unregister short offers receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(offersReceiver);
        // Unregister vehicle state receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vehicleStateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister status bar updates receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusBarUpdatesReceiver);
        soundManager.cancelBeepSound();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationUpdater.stop();
        if (isLoggedIn) {
            tcpClientService.sendBytes(MessengerClient.getLogoutMessage(lastLocation, driverID, this));
            isLoggedIn = false;
        }
        soundManager.release();
        unbindService(serviceConnection);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
            if(Utils.isFragmentVisible(getFragmentManager().findFragmentByTag("TAG_OFFERS_FRAGMENT"))) {
                updateOfferFragments(longOffer);
            } else if(Utils.isFragmentVisible(getFragmentManager().findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT"))) {
                updateMessagesFragments(false);
            }
        } else {
             super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1234 && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.location_permissions_needed, Toast.LENGTH_SHORT).show();
            } else {
                updateStatusBarLocation();
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        // Initialize the location for the login message
        FragmentManager fManager = getFragmentManager();
        LoginFragment loginFragment = (LoginFragment) fManager.findFragmentByTag("TAG_LOGIN_FRAGMENT");
        if(Utils.isFragmentVisible(loginFragment)) {
            loginFragment.initLocation(location);
            hasInitialLocation = true;
        }
        this.lastLocation = location;
        MapFragment mapFragment = (MapFragment) fManager.findFragmentByTag("TAG_MAP_FRAGMENT");
        if (Utils.isFragmentVisible(mapFragment)) {
            mapFragment.updateLocation(lastLocation);
        }
        locationUpdater.setLastLocation(lastLocation);
        if(isWithDiscountClient) {
            discountCalculator.add(lastLocation);
        }
    }

    // Displaying the changes in status of the location services
    private boolean networkAvailable, gpsAvailable;

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
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
            if (tmp == null)
                continue;
            if (bestLocation == null || bestLocation.getAccuracy() > tmp.getAccuracy()) {
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

    // List that keeps most recent popup messages
    private FixedSizeList<PopupMessage> popupMessages = new FixedSizeList<>(10);

    @Override
    public List<PopupMessage> getMessages() {
        return popupMessages.getElements();
    }

    // Gets called from the MessageFragment when a message is manually deleted
    @Override
    public void notifyChange() {
        updateMessagesFragments(false);
    }

    // Broadcast receiver for incoming popup messages
    private BroadcastReceiver popupMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_POPUP_MESSAGE)) {
                // Add the latest message to the list
                byte source = intent.getByteExtra(Parser.SOURCE, (byte) -1);
                String message = intent.getStringExtra(Parser.MESSAGE);
                String timestamp = intent.getStringExtra(Parser.TIMESTAMP);
                popupMessages.insert(new PopupMessage(source, message, timestamp));
                updateMessagesFragments(true);
                soundManager.playMessageSound();
            } else if(action.equals(BroadcastActions.ACTION_INVALID_CARD)) {
                LoginFragment loginFragment = (LoginFragment) getFragmentManager().findFragmentByTag("TAG_LOGIN_FRAGMENT");
                if(Utils.isFragmentVisible(loginFragment)) {
                    loginFragment.setError(getString(R.string.invalid_card_mk));
                }
            }
        }
    };

    private void updateMessagesFragments(boolean wasItemAdded) {
        super.updateMessagesFragments(popupMessages.size(), wasItemAdded);
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

    @Override
    public void onLongOfferFinished() {
        longOffer = null;
        updateOfferFragments(null);

        // Cancel the timers and remove the long offer
        OffersStatusBarFragment offersStatusBarFragment =
                (OffersStatusBarFragment) getFragmentManager().findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
            offersStatusBarFragment.cancelTimers();
        }
        // Remove the client marker
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentByTag("TAG_MAP_FRAGMENT");
        if (mapFragment != null) {
            mapFragment.clear();
        }
    }

    private void updateOfferFragments(LongOffer longOffer) {
        super.updateOfferFragments(longOffer, shortOffers);
    }

    // Broadcast receiver for incoming short offers
    private class OffersReceiver extends BroadcastReceiver {

        private Handler receiverHandler;

        public OffersReceiver(Handler handler) {
            this.receiverHandler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BroadcastActions.ACTION_SHORT_OFFER: {
                    // If there is an active long offer, ignore incoming short offers
                    if (longOffer != null)
                        return;
                    long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                    byte offerSource = intent.getByteExtra(Parser.SOURCE, (byte) -1);
                    String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                    final ShortOffer shortOffer = new ShortOffer(idPhoneCall, offerSource, textMessage);
                    if(Utils.isFragmentVisible(getFragmentManager().findFragmentByTag("TAG_OFFERS_FRAGMENT"))) {
                        shortOffer.read();
                    }
                    shortOffers.add(0, shortOffer);
                    receiverHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!shortOffer.isCanceled()) {
                                shortOffers.remove(shortOffer);
                                updateOfferFragments(null);
                            }
                        }
                    }, 45000);
                    updateOfferFragments(null);
                    soundManager.playShortOfferSound();
                    break;
                }
                case BroadcastActions.ACTION_CANCEL_SHORT_OFFER: {
                    // If there is an active long offer, ignore incoming cancels of short offers
                    if (longOffer != null)
                        return;
                    long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                    String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                    for (final ShortOffer so : shortOffers) {
                        if (so.getIdPhoneCall() == idPhoneCall && !so.isCanceled()) {
                            so.cancel(textMessage);
                            receiverHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    shortOffers.remove(so);
                                    updateOfferFragments(null);
                                }
                            }, 10000);
                            // If the offers fragment is visible, update the list view displaying the short offers
                            FragmentManager fManager = getFragmentManager();
                            OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
                            if(Utils.isFragmentVisible(offersFragment)) {
                                offersFragment.notifyDataSetChanged();
                            }
                        }
                    }
                    break;
                }
                case BroadcastActions.ACTION_LONG_OFFER: {
                    long idPhoneCall = intent.getLongExtra(Parser.ID_PHONE_CALL, -1);
                    float latitude = intent.getFloatExtra(Parser.LATITUDE, -1);
                    float longitude = intent.getFloatExtra(Parser.LONGITUDE, -1);
                    byte offerSource = intent.getByteExtra(Parser.SOURCE, (byte) -1);
                    String textMessage = intent.getStringExtra(Parser.TEXT_MESSAGE);
                    // Accept the long offer if there was already a short offer with the same id
                    for (ShortOffer so : shortOffers) {
                        if (so.getIdPhoneCall() == idPhoneCall && !so.isCanceled()) {
                            longOffer = new LongOffer(idPhoneCall, latitude, longitude, offerSource, textMessage);
                            // Remove all short offers and callbacks scheduled on the handler
                            receiverHandler.removeCallbacksAndMessages(null);
                            shortOffers.clear();
                            // Display the long offer
                            updateOfferFragments(longOffer);
                            soundManager.playLongOfferSound();
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }


    // Broadcast receiver for vehicle state updates that should start
    // a countdown timer.
    private BroadcastReceiver vehicleStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_VEHICLE_STATE_FOR_TIMERS)) {
                int stateId = intent.getIntExtra(Parser.VEHICLE_STATE, -1);
                int timeInState = intent.getIntExtra(Parser.TIME_IN_STATE, 0-1);
                if(stateId != -1 && timeInState != -1) {
                    VehicleState state = VehicleState.getByValue(stateId);
                    OffersStatusBarFragment offersStatusBarFragment =
                            (OffersStatusBarFragment) getFragmentManager().findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
                    if(state == VehicleState.ODI_KON_KLIENT || state == VehicleState.MOVE_TO_CLIENT_NEW_PHONE_CALL) {
                        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
                            offersStatusBarFragment.startCountdownMoveToClient(timeInState);
                        }
                    } else if(state == VehicleState.ZONA_NA_KLIENT || state == VehicleState.WAIT_CLIENT_NEW_PHONE_CALL) {
                        if(offersStatusBarFragment != null && offersStatusBarFragment.isVisible()) {
                            offersStatusBarFragment.startCountdownWaitingClient(timeInState);
                        }
                    }
                }
                OffersStatusBarFragment offersStatusBarFragment =
                        (OffersStatusBarFragment) getFragmentManager().findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
                if(Utils.isFragmentVisible(offersStatusBarFragment)) {
                    offersStatusBarFragment.cancelTimers();
                }
            }
        }
    };

    // Broadcast receiver for status bar updates
    private BroadcastReceiver statusBarUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String value = intent.getStringExtra(TCPClient.VALUE);
            int color = intent.getIntExtra(TCPClient.COLOR, Color.GRAY);
            StatusBarFragment statusBarFragment = (StatusBarFragment) getFragmentManager().findFragmentByTag("TAG_STATUS_BAR_FRAGMENT");
            if(statusBarFragment != null) {
                statusBarFragment.update(action, value, color);
            }
        }
    };

    // Broadcast receiver for login status updates (Successfully logged in/Successfully logged out)
    private BroadcastReceiver loginStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BroadcastActions.ACTION_DRIVER_LOGIN_STATUS)) {
                boolean login = intent.getBooleanExtra(Parser.LOGIN_STATUS, false);
                if(login) {
                    onSuccessfulLogin();
                } else {
                    onSuccessfulLogout();
                }
            }
        }
    };

    @Override
    public void onSuccessfulLogin() {
        if(!isLoggedIn) {
            isLoggedIn = true;
            FragmentManager fManager = getFragmentManager();
            FragmentTransaction fTransaction = fManager.beginTransaction();
            MapFragment mapFragment = new MapFragment();
            mapFragment.initLocation(lastLocation);
            fTransaction.replace(R.id.fragment_content_container, mapFragment, "TAG_MAP_FRAGMENT");
            fTransaction.commit();

            // Start the thread that sends periodical location updates
            locationUpdater.setLastLocation(lastLocation);
            locationUpdater.start();

            // Disable the config button
            ImageButton configButton = (ImageButton) findViewById(R.id.button_config);
            configButton.setEnabled(false);

            // Change the text on the login menu button
            Button loginButton = (Button) findViewById(R.id.button_menu_login_logout);
            loginButton.setText(R.string.logout);

            OffersStatusBarFragment offersStatusBarFragment =
                    (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
            if(offersStatusBarFragment != null) {
                offersStatusBarFragment.setLoggedIn(true);
            }
        }
    }

    @Override
    public void onSuccessfulLogout() {
        if(isLoggedIn) {
            isLoggedIn = false;
            FragmentManager fManager = getFragmentManager();
            // Remove all fragments from the background, if the logout was called
            // when a fragment other than the map was visible(fragment above the map)
            while(fManager.getBackStackEntryCount() > 0)
                fManager.popBackStackImmediate();
            FragmentTransaction fTransaction = fManager.beginTransaction();
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.initLocation(lastLocation);
            fTransaction.replace(R.id.fragment_content_container, loginFragment, "TAG_LOGIN_FRAGMENT");
            fTransaction.commit();

            // Stop the thread that send periodical updates
            locationUpdater.stop();

            // Enable the config button
            ImageButton configButton = (ImageButton) findViewById(R.id.button_config);
            configButton.setEnabled(true);

            // Clear messages, offers and timers
            shortOffers.clear();
            popupMessages.clear();
            longOffer = null;
            updateOfferFragments(null);
            OffersStatusBarFragment offersStatusBarFragment =
                    (OffersStatusBarFragment) getFragmentManager().findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
            if(Utils.isFragmentVisible(offersStatusBarFragment)) {
                offersStatusBarFragment.cancelTimers();
                offersStatusBarFragment.setMessagesCount(popupMessages.size(), false);
                offersStatusBarFragment.setLoggedIn(false);
            }

            // Change the text on the login menu button
            Button loginButton = (Button) findViewById(R.id.button_menu_login_logout);
            loginButton.setText(R.string.login);
        }
    }

    @Override
    public void onDiscountStarted() {
        isWithDiscountClient = true;
        locationUpdater.setBits(false, false, true, false);
    }

    @Override
    public void onDiscountComplete(final double distanceInMeters, final long timeInSeconds) {
        isWithDiscountClient = false;
        locationUpdater.setBits(false, false, false, true);
        soundManager.playBeepSound();
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogLayout = inflater.inflate(R.layout.dialog_discount_ended, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogLayout)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                TextView text = (TextView) dialogLayout.findViewById(R.id.text_view_discount_ended);
                text.setText(String.format(Locale.getDefault(), "Заврши попустот.\nВклучете таксиметар!\n(%.2f/%d)", distanceInMeters, timeInSeconds % 1000));
                Button okButton = (Button) dialogLayout.findViewById(R.id.button_discount_ended);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(tcpClientService.sendBytes(MessengerClient.getCommonMessage(lastLocation, MainActivity.this, false, true, false, false))) {
                            locationUpdater.setBits(false, true, false, false);
                            soundManager.cancelBeepSound();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                tcpClientService.sendBytes(MessengerClient.getCommonMessage(lastLocation, MainActivity.this, false, true, false, false));
                locationUpdater.setBits(false, true, false, false);
                soundManager.cancelBeepSound();
            }
        });
        dialog.show();
    }

    @Override
    public void onDriverIdProvided(String driverId) {
        this.driverID = driverId;
    }
}
