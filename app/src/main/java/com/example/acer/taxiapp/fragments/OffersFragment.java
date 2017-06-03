package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.ShortOffer;
import com.example.acer.taxiapp.TCPClient;
import com.example.acer.taxiapp.Utils;

import java.util.List;

public class OffersFragment extends Fragment {

    private ListView shortOffersList;
    private List<ShortOffer> shortOffers;
    private ShortOffersListAdapter adapter;

    private ShortOffersListProvider provider;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            provider = (ShortOffersListProvider) context;
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
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                provider = (ShortOffersListProvider) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers, container, false);
        shortOffersList = (ListView) view.findViewById(R.id.list_view_short_offers);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        shortOffers = provider.getShortOffers();
        adapter = new ShortOffersListAdapter(getActivity(), shortOffers);
        shortOffersList.setAdapter(adapter);
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    private class ShortOffersListAdapter extends ArrayAdapter<ShortOffer> {

        private Context context;
        private List<ShortOffer> offers;

        ShortOffersListAdapter(@NonNull Context context, @NonNull List<ShortOffer> objects) {
            super(context, 0, objects);
            this.context = context;
            offers = objects;
        }

        @Override
        public int getCount() {
            return offers.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if(view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.list_item_short_offer, parent, false);
            }
            TextView offerSourceTextView = (TextView) view.findViewById(R.id.text_view_list_item_short_offer_source);
            TextView textMessageTextView = (TextView) view.findViewById(R.id.text_view_list_item_short_offer_text);
            Button confirmButton = (Button) view.findViewById(R.id.button_short_offer_confirm);
            Button rejectButton = (Button) view.findViewById(R.id.button_short_offer_reject);
            ShortOffer shortOffer = getItem(position);
            if(shortOffer != null) {
                offerSourceTextView.setText(shortOffer.getOfferSource() == '0' ? "Андроид:" : "Диспечер:" );
                textMessageTextView.setText(shortOffer.getTextMessage());
                final long idPhoneCall = shortOffer.getIdPhoneCall();
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        promptMinutesDialog(idPhoneCall);
                    }
                });
                rejectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
            return view;
        }

        private void promptMinutesDialog(final long idPhoneCall) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            final View view = inflater.inflate(R.layout.dialog_enter_minutes, null);
            builder.setView(view);
            builder.setPositiveButton("Испрати", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText minutesEditText = (EditText) view.findViewById(R.id.edit_text_minutes);
                    String minutesText = minutesEditText.getText().toString();
                    // TODO Validate minutesText
                    int minutes = Integer.parseInt(minutesText);
                    byte[] message = MessengerClient.getShortOfferConfirmMessage(idPhoneCall, minutes, context);
//                    TCPClient tcpClient = TCPClient.getInstance(getActivity());
//                    tcpClient.sendBytes(message);


//                    String ret = "";
//                    for(byte b : message) {
//                        ret += (char) b;
//                    }
//                    Log.e("SHORT_OFFER", "Confirm message : " + ret);
                    Utils.hideKeyboard(minutesEditText, getActivity());
                }
            });
            builder.setNegativeButton("Откажи", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText minutesEditText = (EditText) view.findViewById(R.id.edit_text_minutes);
                    Utils.hideKeyboard(minutesEditText, getActivity());
                    dialog.cancel();
                }
            });
            builder.create().show();
        }
    }

    public interface ShortOffersListProvider {
        List<ShortOffer> getShortOffers();
    }
}
