package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.acer.taxiapp.R;

import java.util.Locale;

public class OffersStatusBarFragment extends Fragment {

    private TextView offersInfoTextView;
    private TextView messagesInfoTextView;
    private int offersCount;
    private int messagesCount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers_status_bar, container, false);
        offersInfoTextView = (TextView) view.findViewById(R.id.text_view_offers_info);
        offersInfoTextView.setText("Нема понуди!");
        offersInfoTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(offersCount == 0)
                    return;
                FragmentManager fManager = getFragmentManager();
                OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
                if(offersFragment != null && offersFragment.isVisible())
                    return;
                boolean popped = fManager.popBackStackImmediate("offers_frag", 0);
                if(!popped) {
                    Log.e("FRAGS", "OFFERS NOT IN STACK");
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new OffersFragment(), "TAG_OFFERS_FRAGMENT");
                    fTransaction.addToBackStack("offers_frag");
                    fTransaction.commit();
                } else {
                    Log.e("FRAGS", "OFFERS IN STACK");
                }
            }
        });
        // Proveruva dali fragmetot koj sakame da go otvorime e vekje na stack
        // Ako e, gi popnuva fragmentite se do ovoj fragment, a ako ne e,
        // go kreira i ja stava transakcijata na stack
        messagesInfoTextView = (TextView) view.findViewById(R.id.text_view_messages_info);
        messagesInfoTextView.setText("Нема пораки!");
        messagesInfoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(messagesCount == 0)
                    return;
                FragmentManager fManager = getFragmentManager();
                MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
                if(messagesFragment != null && messagesFragment.isVisible())
                    return;
                boolean popped = fManager.popBackStackImmediate("messages_frag", 0);
                if(!popped) {
                    Log.e("FRAGS", "MESSAGES NOT IN STACK");
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new MessagesFragment(), "TAG_POPUP_MESSAGES_FRAGMENT");
                    fTransaction.addToBackStack("messages_frag");
                    fTransaction.commit();
                } else {
                    Log.e("FRAGS", "MESSAGES IN STACK");
                }
            }
        });

        return view;
    }

    public void setOffersCount(int count) {
        if(count == 0) {
            offersInfoTextView.setText("Нема најави!");
        } else if(count == -1) {
            offersInfoTextView.setText("Активна долга најава!");
        } else {
            offersInfoTextView.setText(String.format(Locale.getDefault(), "Број на најави: %d", count));
        }
        offersCount = count;
    }

    public void setMessagesCount(int count) {
        if(count == 0) {
            messagesInfoTextView.setText("Нема пораки!");
        } else {
            messagesInfoTextView.setText(String.format(Locale.getDefault(), "Број на пораки: %d", count));
        }
        messagesCount = count;
    }

}
