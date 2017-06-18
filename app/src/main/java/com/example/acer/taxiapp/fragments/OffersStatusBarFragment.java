package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.R;

import java.util.Locale;

public class OffersStatusBarFragment extends Fragment {

    private TextView timerTextView;
    private TextView offersInfoTextView;
    private TextView messagesInfoTextView;

    private CustomCountDownTimer timerMoveToClient;
    private CustomCountDownTimer timerWaitingClient;
    private int offersCount;
    private int messagesCount;

    private boolean isLoggedIn = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers_status_bar, container, false);
        timerTextView = (TextView) view.findViewById(R.id.text_view_timer);
        offersInfoTextView = (TextView) view.findViewById(R.id.text_view_offers_info);
        offersInfoTextView.setText(R.string.no_offers);
        offersInfoTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!isLoggedIn) {
                    Toast.makeText(getActivity(), R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
                    return;
                }
                FragmentManager fManager = getFragmentManager();
                OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
                if(offersFragment != null && offersFragment.isVisible())
                    return;
                boolean popped = fManager.popBackStackImmediate("offers_frag", 0);
                if(!popped) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new OffersFragment(), "TAG_OFFERS_FRAGMENT");
                    fTransaction.addToBackStack("offers_frag");
                    fTransaction.commit();
                }
            }
        });
        // Proveruva dali fragmetot koj sakame da go otvorime e vekje na stack
        // Ako e, gi popnuva fragmentite se do ovoj fragment, a ako ne e,
        // go kreira i ja stava transakcijata na stack
        messagesInfoTextView = (TextView) view.findViewById(R.id.text_view_messages_info);
        messagesInfoTextView.setText(R.string.no_messages);
        messagesInfoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoggedIn) {
                    Toast.makeText(getActivity(), R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
                    return;
                }
                FragmentManager fManager = getFragmentManager();
                MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
                if(messagesFragment != null && messagesFragment.isVisible())
                    return;
                boolean popped = fManager.popBackStackImmediate("messages_frag", 0);
                if(!popped) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new MessagesFragment(), "TAG_POPUP_MESSAGES_FRAGMENT");
                    fTransaction.addToBackStack("messages_frag");
                    fTransaction.commit();
                }
            }
        });

        return view;
    }

    public void setOffersCount(int count) {
        if(count == 0) {
            offersInfoTextView.setText(R.string.no_offers);
        } else if(count == -1) {
            offersInfoTextView.setText(R.string.active_long_offer);
        } else {
            offersInfoTextView.setText(String.format(Locale.getDefault(), getString(R.string.number_of_offers), count));
        }
        offersCount = count;
    }

    public void setMessagesCount(int count) {
        if(count == 0) {
            messagesInfoTextView.setText(R.string.no_messages);
        } else {
            messagesInfoTextView.setText(String.format(Locale.getDefault(), getString(R.string.number_of_messages), count));
        }
        messagesCount = count;
    }

    public void startCountdownMoveToClient(int minutes) {
        timerTextView.setTextColor(Color.WHITE);
        timerTextView.setVisibility(View.VISIBLE);
        if(timerMoveToClient == null) {
            timerMoveToClient = new CustomCountDownTimer(minutes * 60 * 1000, 1000, getString(R.string.move_to_client));
            timerMoveToClient.start();
        } else {
            if(timerMoveToClient.isRunning()) {
                timerMoveToClient.cancel();
            }
            timerMoveToClient = new CustomCountDownTimer(minutes * 60 * 1000, 1000, getString(R.string.move_to_client));
            timerMoveToClient.start();
        }
    }

    public void startCountdownWaitingClient(int minutes) {
        timerTextView.setTextColor(Color.WHITE);
        timerTextView.setVisibility(View.VISIBLE);
        if(timerMoveToClient != null && timerMoveToClient.isRunning()) {
            timerMoveToClient.cancel();
        }
        timerWaitingClient = new CustomCountDownTimer(minutes * 60 * 1000, 1000, getString(R.string.waiting));
        timerWaitingClient.start();
    }

    public void cancelTimers() {
        if(timerMoveToClient != null && timerMoveToClient.isRunning()) {
            timerMoveToClient.cancel();
        }
        if(timerWaitingClient != null && timerWaitingClient.isRunning()) {
            timerWaitingClient.cancel();
        }
        timerTextView.setText("");
        timerTextView.setVisibility(View.INVISIBLE);
    }


    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    private class CustomCountDownTimer extends CountDownTimer {

        private boolean isRunning;
        private String description;

        CustomCountDownTimer(long millisInFuture, long countDownInterval, String description) {
            super(millisInFuture, countDownInterval);
            this.description = description;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            isRunning = true;
            long totalSeconds = millisUntilFinished / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            timerTextView.setText(String.format(Locale.getDefault(), "%s %02d:%02d", description, minutes, seconds));
        }

        @Override
        public void onFinish() {
            isRunning = false;
            timerTextView.setText(String.format("%s %s", description, getResources().getString(R.string.timer_zero)));
            timerTextView.setTextColor(Color.RED);
        }

        synchronized boolean isRunning() {
            return isRunning;
        }
    }
}
