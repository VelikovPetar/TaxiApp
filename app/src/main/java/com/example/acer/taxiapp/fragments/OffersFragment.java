package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.models.LongOffer;
import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.models.ShortOffer;
import com.example.acer.taxiapp.tcp.TCPClient;

import java.util.List;

public class OffersFragment extends Fragment {

    private TextView noOffersTextView;
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
        noOffersTextView = (TextView) view.findViewById(R.id.text_view_no_offers);
        shortOffersList = (ListView) view.findViewById(R.id.list_view_short_offers);
        longOfferLayout = (LinearLayout) view.findViewById(R.id.linear_layout_long_offer);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        shortOffers = provider.getShortOffers();
        for(ShortOffer so: shortOffers) {
            so.read();
        }
        adapter = new ShortOffersListAdapter(getActivity(), shortOffers);
        shortOffersList.setAdapter(adapter);
        longOffer = provider.getLongOffer();
        if(longOffer != null) {
            displayLongOffer(longOffer);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleNoOffersTextView();
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    public void displayLongOffer(LongOffer _longOffer) {
        longOffer = _longOffer;
        TextView textMessageTextView = (TextView) longOfferLayout.findViewById(R.id.text_view_long_offer_text);
        Button confirmButton = (Button) longOfferLayout.findViewById(R.id.button_long_offer_confirm);
        textMessageTextView.setText(longOffer.getTextMessage());
        final float latitude = longOffer.getLatitude();
        final float longitude = longOffer.getLongitude();
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fManager = getFragmentManager();
                MapFragment mapFragment = (MapFragment) fManager.findFragmentByTag("TAG_MAP_FRAGMENT");
                mapFragment.setCustomerLatLng(latitude, longitude);

                // The map is always at the bottom
                while(fManager.getBackStackEntryCount() > 0)
                    fManager.popBackStackImmediate();
            }
        });
        longOfferLayout.setVisibility(View.VISIBLE);
        toggleNoOffersTextView();
    }

    public void hideLongOffer() {
        longOffer = null;
        longOfferLayout.setVisibility(View.GONE);
        toggleNoOffersTextView();
    }

    private void toggleNoOffersTextView() {
        if(shortOffers.size() == 0 && longOffer == null) {
            noOffersTextView.setVisibility(View.VISIBLE);
        } else {
            noOffersTextView.setVisibility(View.GONE);
        }
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
            TextView textMessageTextView = (TextView) view.findViewById(R.id.text_view_list_item_short_offer_text);
            final Button confirmButton = (Button) view.findViewById(R.id.button_short_offer_confirm);
            final ShortOffer shortOffer = getItem(position);
            if(shortOffer != null) {
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
                if(shortOffer.isAccepted()) {
                    confirmButton.setEnabled(false);
                }
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        promptMinutesDialog(shortOffer, confirmButton);
                        promptMinutesDialogRadio(shortOffer, confirmButton);
                    }
                });

            }
            return view;
        }

        private void promptMinutesDialog(final ShortOffer shortOffer, final Button button) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            final View dialogLayout = layoutInflater.inflate(R.layout.dialog_enter_minutes, null);
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.send, null)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
                                minutesEditText.setHint(R.string.error_enter_minutes);
                                minutesEditText.setHintTextColor(Color.RED);
                                return;
                            }
                            int minutes = Integer.parseInt(minutesText.trim());
                            byte[] message = MessengerClient.getShortOfferConfirmMessage(shortOffer.getIdPhoneCall(), minutes, context);
                            TCPClient tcpClient = TCPClient.getInstance(getActivity());
                            if(!tcpClient.sendBytes(message)) {
                                Toast.makeText(getActivity(), R.string.error_sending_message, Toast.LENGTH_LONG).show();
                                return;
                            }
                            button.setEnabled(false);
                            shortOffer.accept();
                            dialog.dismiss();
                        }
                    });
                }
            });
            dialog.show();
        }
    }

    private void promptMinutesDialogRadio(final ShortOffer shortOffer, final Button button) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_enter_minutes_radio, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(dialogLayout)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RadioGroup radioGroup = (RadioGroup) dialogLayout.findViewById(R.id.radio_group_minutes);
                        int checkedId = radioGroup.getCheckedRadioButtonId();
                        int minutes = 2;
                        switch (checkedId) {
                            case R.id.radio_button_minutes_2:
                                minutes = 2;
                                break;
                            case R.id.radio_button_minutes_4:
                                minutes = 4;
                                break;
                            case R.id.radio_button_minutes_6:
                                minutes = 6;
                                break;
                            case R.id.radio_button_minutes_8:
                                minutes = 8;
                                break;
                            case R.id.radio_button_minutes_10:
                                minutes = 10;
                                break;
                        }
                        byte[] message = MessengerClient.getShortOfferConfirmMessage(shortOffer.getIdPhoneCall(), minutes, getActivity());
                        TCPClient tcpClient = TCPClient.getInstance(getActivity());
                        if(!tcpClient.sendBytes(message)) {
                            Toast.makeText(getActivity(), R.string.error_sending_message, Toast.LENGTH_LONG).show();
                            return;
                        }
                        shortOffer.accept();
                        button.setEnabled(false);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.show();
    }

    public interface ShortOffersListProvider {
        List<ShortOffer> getShortOffers();
        LongOffer getLongOffer();
        void onLongOfferFinished();
    }
}
