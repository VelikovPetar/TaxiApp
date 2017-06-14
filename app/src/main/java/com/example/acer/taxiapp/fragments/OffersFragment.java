package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.LongOffer;
import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.ShortOffer;
import com.example.acer.taxiapp.TCPClient;
import com.example.acer.taxiapp.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class OffersFragment extends Fragment {

    private ListView shortOffersList;
    private List<ShortOffer> shortOffers;
    private ShortOffersListAdapter adapter;

    private ShortOffersListProvider provider;

    private LinearLayout longOfferLayout;
    private LongOffer longOffer;

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
        longOfferLayout = (LinearLayout) view.findViewById(R.id.linear_layout_long_offer);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        shortOffers = provider.getShortOffers();
        adapter = new ShortOffersListAdapter(getActivity(), shortOffers);
        shortOffersList.setAdapter(adapter);
        longOffer = provider.getLongOffer();
        if(longOffer != null) {
            displayLongOffer(longOffer);
        }
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    public void displayLongOffer(LongOffer _longOffer) {
        longOffer = _longOffer;
        TextView offerSourceTextView = (TextView) longOfferLayout.findViewById(R.id.text_view_long_offer_source);
        TextView textMessageTextView = (TextView) longOfferLayout.findViewById(R.id.text_view_long_offer_text);
        Button confirmButton = (Button) longOfferLayout.findViewById(R.id.button_long_offer_confirm);
        offerSourceTextView.setText(longOffer.getOfferSource() == '0' ? "Андроид:" : "Диспечер:");
        textMessageTextView.setText(longOffer.getTextMessage());
        final float latitude = longOffer.getLatitude();
        final float longitude = longOffer.getLongitude();
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fManager = getFragmentManager();
                FragmentTransaction fTransaction = fManager.beginTransaction();
                MapFragment mapFragment = (MapFragment) fManager.findFragmentByTag("TAG_MAP_FRAGMENT");
                mapFragment.setCustomerLatLng(latitude, longitude);
//                if(mapFragment.isAdded()) {
//                    fTransaction.show(mapFragment);
//                } else {
//                    fTransaction.replace(R.id.fragment_content_container, mapFragment, "TAG_MAP_FRAGMENT");
//                }

                // Popni ja poslednata transakcija od stackot, bidejki pod nea se naogja mapata
                // Nikogash nema da ima povekje od edna transakcija na stackot, a najdole sekad e mapata
                fManager.popBackStack();
                fTransaction.commit();
            }
        });
        longOfferLayout.setVisibility(View.VISIBLE);
        Log.e("LONG OFFER", "Display called");
    }

    public void hideLongOffer() {
        longOffer = null;
        longOfferLayout.setVisibility(View.GONE);
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
            ShortOffer shortOffer = getItem(position);
            if(shortOffer != null) {
                offerSourceTextView.setText(shortOffer.getOfferSource() == '0' ? "Андроид:" : "Диспечер:");
                textMessageTextView.setText(shortOffer.getTextMessage());

                // If the offer is cancelled from the server, display it with different color and
                // disable the confirm button
                if(shortOffer.isCanceled()) {
                    view.setBackgroundColor(Color.DKGRAY);
                    confirmButton.setEnabled(false);
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    confirmButton.setEnabled(true);
                }
                final long idPhoneCall = shortOffer.getIdPhoneCall();
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        promptMinutesDialog(idPhoneCall);
                    }
                });
            }
            return view;
        }

        private void promptMinutesDialog(final long idPhoneCall) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            final View dialogLayout = layoutInflater.inflate(R.layout.dialog_enter_minutes, null);
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(dialogLayout)
                    .setPositiveButton("Испрати", null)
                    .setNegativeButton("Откажи", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface _dialog) {
                    Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText minutesEditText = (EditText) dialogLayout.findViewById(R.id.edit_text_minutes);
                            String minutesText = minutesEditText.getText().toString();
                            if(minutesText.trim().equals("")) {
                                minutesEditText.setHint("Мора да внесете минути!");
                                minutesEditText.setHintTextColor(Color.RED);
                                return;
                            }
                            int minutes = Integer.parseInt(minutesText.trim());
                            byte[] message = MessengerClient.getShortOfferConfirmMessage(idPhoneCall, minutes, context);
//                            TCPClient tcpClient = TCPClient.getInstance(context);
//                            tcpClient.sendBytes(message);
                            String msg = "";
                            for(byte b : message)
                                msg += (char) b;
                            long l = ByteBuffer.wrap(Arrays.copyOfRange(message, 9, 13)).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            Log.e("DIALOGS", msg + " " + l);
                            dialog.dismiss();
                        }
                    });
                }
            });
            dialog.show();
        }
    }

    public interface ShortOffersListProvider {
        List<ShortOffer> getShortOffers();
        LongOffer getLongOffer();
        void onLongOfferFinished();
    }
}
