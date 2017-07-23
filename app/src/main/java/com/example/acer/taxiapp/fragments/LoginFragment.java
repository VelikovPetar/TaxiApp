package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.activity.MainActivity;
import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.tcp.TCPClient;
import com.example.acer.taxiapp.utils.Utils;

public class LoginFragment extends Fragment {

    private EditText loginEditText;
    private TextView errorTextView;
    private TextView noServicesTextView;
    private Button loginButton;

    private Location location;

    private boolean isConfig = true;

    // Reference to MainActivity for providing the driver ID
    private DriverIdProvider provider;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            provider = (DriverIdProvider) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }


    // If the device has android version older than 6.0(Marshmallow),
    // the method onAttach(context) doesn't get called.
    // On devices with android version 6.0 or newer, both methods
    // onAttach(Context) and onAttach(Activity) are called.
    // This method performs the check so the provider doesn't get
    // initialized twice.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                provider = (DriverIdProvider) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.hasInternetConnection(getActivity()) &&
                    Utils.isLocationEnabled(getActivity()) &&
                    location != null) {
                enableViews();
            } else {
                disableViews();
            }
            errorTextView.setVisibility(View.INVISIBLE);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        loginEditText = (EditText) view.findViewById(R.id.edit_text_login);
        errorTextView = (TextView) view.findViewById(R.id.text_view_error_login);
        errorTextView.setVisibility(View.INVISIBLE);
        noServicesTextView = (TextView) view.findViewById(R.id.text_view_login_no_required_services);
        noServicesTextView.setVisibility(View.INVISIBLE);
        loginButton = (Button) view.findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfig) {
                    String driverId = loginEditText.getText().toString().trim();
                    if(driverId.equals("")) {
                        return;

                    }
                    provider.onDriverIdProvided(driverId);
                    TCPClient tcpClient = TCPClient.getInstance(getActivity());
                    if (tcpClient.sendBytes(MessengerClient.getLoginMessage(location, driverId, getActivity()))) {
                        errorTextView.setText(R.string.waiting_response);
                        errorTextView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getActivity(), R.string.error_sending_message, Toast.LENGTH_LONG).show();
                    }
                    View view = getActivity().getCurrentFocus();
                    Utils.hideKeyboard(view);
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        setup();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction("android.location.PROVIDERS_CHANGED");
        getActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void setError(String error) {
        errorTextView.setText(error);
        errorTextView.setVisibility(View.VISIBLE);
    }

    public void initLocation(Location location) {
        this.location = location;
        if (isResumed())
            setup();
    }

    private void setup() {
        disableViews();
        noServicesTextView.setVisibility(View.INVISIBLE);
        errorTextView.setTextColor(Color.RED);
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        if (!preferences.contains(MainActivity.DEVICE_ID)) {
            errorTextView.setText(R.string.error_no_config_found);
            errorTextView.setVisibility(View.VISIBLE);
            isConfig = false;
            return;
        }
        isConfig = true;
        if (!Utils.isLocationEnabled(getActivity())) {
            noServicesTextView.setVisibility(View.VISIBLE);
            return;
        }
        if (!Utils.hasInternetConnection(getActivity())) {
            noServicesTextView.setVisibility(View.VISIBLE);
            return;
        }
        if (location == null) {
            return;
        }
        noServicesTextView.setVisibility(View.INVISIBLE);
        enableViews();
    }

//    private void promptLocationServices() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Location Services");
//        builder.setMessage("Turn on location services to enable login.");
//        builder.setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivity(intent);
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        builder.create().show();
//    }

//    private void promptInternetConnection() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Internet Connection");
//        builder.setMessage("Turn on internet connection to enable login.");
//        builder.setPositiveButton("WiFi", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
//                startActivity(intent);
//            }
//        });
//        builder.setNeutralButton("Mobile Data", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent();
//                intent.setComponent(new ComponentName(
//                            "com.android.settings",
//                            "com.android.settings.Settings$DataUsageSummaryActivity"));
//                startActivity(intent);
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        builder.create().show();
//    }

    private void enableViews() {
        loginEditText.setEnabled(true);
        loginButton.setEnabled(true);
        noServicesTextView.setVisibility(View.INVISIBLE);
    }

    private void disableViews() {
        loginEditText.setEnabled(false);
        loginButton.setEnabled(false);
        noServicesTextView.setVisibility(View.VISIBLE);
    }

    public interface DriverIdProvider {
        void onDriverIdProvided(String driverId);
    }
}
