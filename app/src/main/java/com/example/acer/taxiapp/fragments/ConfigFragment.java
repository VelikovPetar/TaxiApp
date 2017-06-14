package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.acer.taxiapp.MainActivity;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.Utils;

public class ConfigFragment extends Fragment {

    private EditText driverNumberEditText;
    private EditText deviceNumberEditText;
    private Button confirmButton;
    private TextView errorTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        driverNumberEditText = (EditText) view.findViewById(R.id.edit_text_setup_driver_code);
        deviceNumberEditText = (EditText) view.findViewById(R.id.edit_text_setup_device_code);
        errorTextView = (TextView) view.findViewById(R.id.text_view_error_config);
        confirmButton = (Button) view.findViewById(R.id.button_confirm_config);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean shouldCommit = true;
                String errorMessage = "";
                if(driverNumberEditText.getText().toString().trim().length() != 4) {
                    errorMessage += getString(R.string.rf_card_error);
                    shouldCommit = false;
                }
                if(deviceNumberEditText.getText().toString().trim().length() != 5) {
                    errorMessage += getString(R.string.device_id_error);
                    shouldCommit = false;
                }
                if(!shouldCommit) {
                    errorTextView.setText(errorMessage);
                    errorTextView.setTextColor(Color.RED);
                } else {
                    SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(MainActivity.RF_CARD_ID, driverNumberEditText.getText().toString().trim());
                    editor.putString(MainActivity.DEVICE_ID, deviceNumberEditText.getText().toString().trim());
                    editor.apply();
                    errorTextView.setText(R.string.configuration_success);
                    errorTextView.setTextColor(Color.GREEN);

                    // Hide keyboard
                    View view = getActivity().getCurrentFocus();
                    Utils.hideKeyboard(view);
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        if(preferences.contains(MainActivity.RF_CARD_ID) && preferences.contains(MainActivity.DEVICE_ID)) {
            errorTextView.setText(R.string.override_prev_configuration);
            errorTextView.setTextColor(Color.YELLOW);
        }
    }
}
