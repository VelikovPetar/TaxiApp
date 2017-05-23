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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.acer.taxiapp.MainActivity;
import com.example.acer.taxiapp.R;

/**
 * Created by Acer on 17.5.2017.
 */

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
                    errorMessage += "Бројот на Rf картичката мора да е со должина 4.";
                    shouldCommit = false;
                }
                if(deviceNumberEditText.getText().toString().trim().length() != 5) {
                    errorMessage += "Бројот на уредот мора да е со должина 5.";
                    shouldCommit = false;
                }
                if(!shouldCommit) {
                    errorTextView.setText(errorMessage);
                    errorTextView.setTextColor(Color.RED);
                } else {
                    SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, 0);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(MainActivity.RF_CARD_ID, driverNumberEditText.getText().toString().trim());
                    editor.putString(MainActivity.DEVICE_ID, deviceNumberEditText.getText().toString().trim());
                    editor.apply();
                    errorTextView.setText("Успешна конфигурација!");
                    errorTextView.setTextColor(Color.GREEN);

                    // Hide keyboard
                    // TODO Extract method for hiding keyboard from everywhere
                    View view = getActivity().getCurrentFocus();
                    if(view != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, 0);
        if(preferences.contains(MainActivity.RF_CARD_ID) || preferences.contains(MainActivity.DEVICE_ID)) {
            errorTextView.setText("Уредот е веќе конфигуриран. Внесување нови вредности ќе ги препокри старите.");
            errorTextView.setTextColor(Color.YELLOW);
        }
    }
}
