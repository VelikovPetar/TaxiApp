package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.acer.taxiapp.R;

/**
 * Created by Acer on 12.5.2017.
 */

public class LoginFragment extends Fragment {

    private EditText loginEditText;
    private TextView loginErrorTextView;
    private Button loginButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        loginEditText = (EditText) view.findViewById(R.id.edit_text_login);
        loginErrorTextView = (TextView) view.findViewById(R.id.text_view_error_login);
        loginErrorTextView.setVisibility(View.INVISIBLE);
        loginButton = (Button) view.findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loginEditText.getText().toString().equals("1234")) {
                    hideKeyboard();
                    loginErrorTextView.setVisibility(View.INVISIBLE);
                    FragmentManager fManager = getFragmentManager();
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new MapFragment());
                    fTransaction.commit();
                } else {
                    loginErrorTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        return view;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(loginEditText.getWindowToken(), 0);
    }
}
