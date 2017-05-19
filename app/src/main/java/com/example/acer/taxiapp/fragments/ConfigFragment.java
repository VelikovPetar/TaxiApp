package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.acer.taxiapp.R;

/**
 * Created by Acer on 17.5.2017.
 */

public class ConfigFragment extends Fragment {

    private EditText driverNumberEditText;
    private EditText deviceNumberEditText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        driverNumberEditText = (EditText) view.findViewById(R.id.edit_text_setup_driver_code);
        deviceNumberEditText = (EditText) view.findViewById(R.id.edit_text_setup_device_code);
        return view;
    }
}
