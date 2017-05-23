package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
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
 * Created by Acer on 12.5.2017.
 */

public class LoginFragment extends Fragment {

    private EditText loginEditText;
    private TextView errorTextView;
    private Button loginButton;

    private boolean isConfig = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        loginEditText = (EditText) view.findViewById(R.id.edit_text_login);
        errorTextView = (TextView) view.findViewById(R.id.text_view_error_login);
        errorTextView.setVisibility(View.INVISIBLE);
        loginButton = (Button) view.findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConfig) {
                    SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
                    String driverID = preferences.getString(MainActivity.RF_CARD_ID, null);
                    if(driverID != null && driverID.equals(loginEditText.getText().toString().trim())) {
                        FragmentManager fManager = getFragmentManager();
                        FragmentTransaction fTransaction = fManager.beginTransaction();
                        fTransaction.replace(R.id.fragment_content_container, new MapFragment());
                        fTransaction.commit();
                        hideKeyboard();
                        errorTextView.setVisibility(View.INVISIBLE);
                    } else {
                        errorTextView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        if(!preferences.contains(MainActivity.RF_CARD_ID) || !preferences.contains(MainActivity.DEVICE_ID)) {
            errorTextView.setText("Уредот не е конфигуриран! Направете конфигурација пред да се логирате!");
            errorTextView.setVisibility(View.VISIBLE);
            isConfig = false;
            loginEditText.setEnabled(false);
            loginButton.setEnabled(false);
        }
    }

    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
