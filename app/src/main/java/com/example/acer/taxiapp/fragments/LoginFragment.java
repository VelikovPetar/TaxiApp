package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.acer.taxiapp.MainActivity;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.TCPClient;
import com.example.acer.taxiapp.Utils;

public class LoginFragment extends Fragment {

    private EditText loginEditText;
    private TextView errorTextView;
    private TextView noServicesTextView;
    private Button loginButton;

    private boolean isConfig = true;
    private LoginCallbacks callbacks;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("LOGIN", "RECEVED");
            if(Utils.hasInternetConnection(getActivity()) &&
                    Utils.isLocationEnabled(getActivity())
                    ) {
                enableViews();
            } else {
                disableViews();
            }
            errorTextView.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callbacks = (LoginCallbacks) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
            Log.e("LOGIN_FRAGMENT", "Class Cast Exception");
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
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e("LOGIN_FRAGMENT", "On Attach(activity)");
            try {
                callbacks = (LoginCallbacks) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
                Log.e("LOGIN_FRAGMENT", "Class Cast Exception");
            }
        }
    }

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
                if(isConfig) {
                    SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
                    String driverID = preferences.getString(MainActivity.RF_CARD_ID, null);
                    if(driverID != null && driverID.equals(loginEditText.getText().toString().trim())) {
                        View view = getActivity().getCurrentFocus();
                        Utils.hideKeyboard(view, getActivity());
                        errorTextView.setText(getString(R.string.login_error));
                        errorTextView.setVisibility(View.INVISIBLE);
                        callbacks.onSuccessfulLogin();
                    } else {
                        errorTextView.setVisibility(View.VISIBLE);
                    }
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

    private void setup() {
        disableViews();
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        if(!preferences.contains(MainActivity.RF_CARD_ID) || !preferences.contains(MainActivity.DEVICE_ID)) {
            errorTextView.setText("Уредот не е конфигуриран! Направете конфигурација пред да се логирате!");
            errorTextView.setVisibility(View.VISIBLE);
            isConfig = false;
            return;
        }
        if(!Utils.isLocationEnabled(getActivity())) {
//            promptLocationServices();
            Log.e("LOGIN", 1+"");
            return;
        }
        if(!Utils.hasInternetConnection(getActivity())) {
//            promptInternetConnection();
            Log.e("LOGIN", 2+"");

            return;
        }
        enableViews();
    }

    private void promptLocationServices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Location Services");
        builder.setMessage("Turn on location services to enable login.");
        builder.setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    private void promptInternetConnection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Internet Connection");
        builder.setMessage("Turn on internet connection to enable login.");
        builder.setPositiveButton("WiFi", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNeutralButton("Mobile Data", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                            "com.android.settings",
                            "com.android.settings.Settings$DataUsageSummaryActivity"));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

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

    public interface LoginCallbacks {
        void onSuccessfulLogin();
    }
}
