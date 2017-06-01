package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.ShortOffer;

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
                view.setTag(shortOffer.getIdPhoneCall()); // TODO
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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
    }

    public interface ShortOffersListProvider {
        List<ShortOffer> getShortOffers();
    }
}
