package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
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
                if (isConfig()) {
                    String driverId = loginEditText.getText().toString().trim();
                    if (driverId.equals("")) {
                        return;

                    }
                    if (!Utils.hasInternetConnection(getActivity()) || !Utils.isLocationEnabled(getActivity()) || location == null) {
                        errorTextView.setText(R.string.login_error2);
                        errorTextView.setVisibility(View.VISIBLE);
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
                } else {
                    errorTextView.setText(R.string.error_no_config_found);
                    errorTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        return view;
    }

    public void setError(String error) {
        errorTextView.setText(error);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private boolean isConfig() {
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        return preferences.contains(MainActivity.DEVICE_ID);
    }

    public void initLocation(Location location) {
        this.location = location;
    }

    public interface DriverIdProvider {
        void onDriverIdProvided(String driverId);
    }
}
