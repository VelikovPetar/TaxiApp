package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.acer.taxiapp.R;

public class OffersFragment extends Fragment {

    private TextView offersInfoTextView;
    private TextView messagesInfoTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers, container, false);
        offersInfoTextView = (TextView) view.findViewById(R.id.text_view_offers_info);
        offersInfoTextView.setText("Нема понуди!");

        messagesInfoTextView = (TextView) view.findViewById(R.id.text_view_messages_info);
        messagesInfoTextView.setText("Нема пораки!");

        return view;
    }

}
