package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.TCPClient;
import com.example.acer.taxiapp.Utils;

public class CheckCardFragment extends Fragment {

    private EditText cardEditText;
    private Button checkCardButton;
    private Location location;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_check_card, container, false);
        cardEditText = (EditText) view.findViewById(R.id.edit_text_check_card);
        checkCardButton = (Button) view.findViewById(R.id.button_check_card);
        checkCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = cardEditText.getText().toString().trim();
                if(!text.equals("")) {
                    byte[] message = MessengerClient.getLoginMessage(location, text, getActivity());
                    TCPClient tcpClient = TCPClient.getInstance(getActivity());
                    if(!tcpClient.sendBytes(message)) {
                        Toast.makeText(getActivity(), R.string.error_sending_message, Toast.LENGTH_LONG).show();
                    }
                    View view = getActivity().getCurrentFocus();
                    Utils.hideKeyboard(view);
                }
            }
        });
        return view;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
