package com.example.acer.taxiapp.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.utils.Utils;

import java.util.Locale;

public class OffersStatusBarFragment extends Fragment {

    private TextView timerTextView;
    private Button offersInfoButton;
    private Button messagesInfoButton;

    private CustomCountDownTimer timerMoveToClient;
    private CustomCountDownTimer timerWaitingClient;
    private CountUpTimer lateTimer;

    private int totalOffers = 0, totalMessages = 0, unreadOffers = 0, unreadMessages = 0;

    private boolean isLoggedIn = false;
    private boolean hasLongOffer = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers_status_bar, container, false);
        timerTextView = (TextView) view.findViewById(R.id.text_view_timer);
        // Proveruva dali fragmetot koj sakame da go otvorime e vekje na stack
        // Ako e, gi popnuva fragmentite se do ovoj fragment, a ako ne e,
        // go kreira i ja stava transakcijata na stack

        offersInfoButton = (Button) view.findViewById(R.id.button_offers_info);
        offersInfoButton.setText(String.format(Locale.getDefault(), "Најави: %d/%d", unreadOffers, totalOffers));
        offersInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoggedIn) {
                    Toast.makeText(getActivity(), R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
                    return;
                }
                FragmentManager fManager = getFragmentManager();
                OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
                if(Utils.isFragmentVisible(offersFragment))
                    return;
                boolean popped = fManager.popBackStackImmediate("offers_frag", 0);
                if(!popped) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new OffersFragment(), "TAG_OFFERS_FRAGMENT");
                    fTransaction.addToBackStack("offers_frag");
                    fTransaction.commit();
                }
                if(hasLongOffer) {
                    setLongOfferActive();
                } else {
                    setUnreadOffers(0);
                }
            }
        });
        messagesInfoButton = (Button) view.findViewById(R.id.button_messages_info);
        messagesInfoButton.setText(String.format(Locale.getDefault(), "Пораки: %d/%d", unreadMessages, totalMessages));
        messagesInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoggedIn) {
                    Toast.makeText(getActivity(), R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
                    return;
                }
                FragmentManager fManager = getFragmentManager();
                MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
                if(Utils.isFragmentVisible(messagesFragment))
                    return;
                boolean popped = fManager.popBackStackImmediate("messages_frag", 0);
                if(!popped) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    fTransaction.replace(R.id.fragment_content_container, new MessagesFragment(), "TAG_POPUP_MESSAGES_FRAGMENT");
                    fTransaction.addToBackStack("messages_frag");
                    fTransaction.commit();
                }
                setMessagesCount(totalMessages, false);
            }
        });
        return view;
    }

    public void setUnreadOffers(int unread) {
        this.unreadOffers = unread;
        setOffersButtonBackground();
    }

    public void setTotalOffers(int total) {
        this.totalOffers = total;
        setOffersButtonBackground();
    }

    public void setHasLongOffer(boolean hasLongOffer) {
        this.hasLongOffer = hasLongOffer;
        if(this.hasLongOffer) {
            setLongOfferActive();
        }
    }

    private void setLongOfferActive() {
        setUnreadOffers(0);
        setTotalOffers(0);
        offersInfoButton.setText(R.string.active_long_offer);
        offersInfoButton.setBackground(Utils.getDrawable(getActivity(), R.drawable.rounded_red));
    }

    public void setMessagesCount(int total, boolean wasItemAdded) {
        if(total == 0) {
            unreadMessages = 0;
        } else if(wasItemAdded) {
            // Ako MessagesFragment e vidliv, ne go zgolemuvaj brojot na neprochitani poraki
            FragmentManager fManager = getFragmentManager();
            MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
            if(!Utils.isFragmentVisible(messagesFragment)) {
                unreadMessages++;
                unreadMessages = Math.min(unreadMessages, total);
            }
        } else {
            // Ako item ne bil dodaden, ili bil izbrishan
            unreadMessages = 0;
        }
        totalMessages = total;
        setMessagesButtonBackground();
    }

    private void setOffersButtonBackground() {
        int backgroundId;
        if(hasLongOffer) {
            backgroundId = R.drawable.rounded_red;
        } else if(unreadOffers == 0) {
            backgroundId = R.drawable.rounded_green;
        } else {
            backgroundId = R.drawable.rounded_yellow;
        }
        offersInfoButton.setBackground(Utils.getDrawable(getActivity(), backgroundId));
        offersInfoButton.setText(String.format(Locale.getDefault(), "Најави: %d/%d", unreadOffers, totalOffers));
    }

    private void setMessagesButtonBackground() {
        int backgroundId;
        if(unreadMessages == 0) {
            backgroundId = R.drawable.rounded_green;
        } else {
            backgroundId = R.drawable.rounded_yellow;
        }
        messagesInfoButton.setBackground(Utils.getDrawable(getActivity(), backgroundId));
        messagesInfoButton.setText(String.format(Locale.getDefault(), "Пораки: %d/%d", unreadMessages, totalMessages));
    }

    public void startCountdownMoveToClient(int minutes) {
        cancelTimers();
        timerTextView.setTextColor(Color.WHITE);
        timerTextView.setVisibility(View.VISIBLE);
        timerMoveToClient = new CustomCountDownTimer(minutes * 60 * 1000, 1000, getString(R.string.move_to_client));
        timerMoveToClient.start();

    }

    public void startCountdownWaitingClient(int minutes) {
        cancelTimers();
        timerTextView.setTextColor(Color.WHITE);
        timerTextView.setVisibility(View.VISIBLE);
        timerWaitingClient = new CustomCountDownTimer(minutes * 60 * 1000, 1000, getString(R.string.waiting));
        timerWaitingClient.start();
    }

    public void cancelTimers() {
        Log.e("TIMERS", "Cancelling timers");
        if(timerMoveToClient != null && timerMoveToClient.isRunning()) {
            timerMoveToClient.cancel();
        }
        timerMoveToClient = null;
        if(timerWaitingClient != null && timerWaitingClient.isRunning()) {
            timerWaitingClient.cancel();
        }
        timerWaitingClient = null;
        if(lateTimer != null && lateTimer.isRunning()) {
            lateTimer.cancel();
        }
        lateTimer = null;
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
            // Start timer that counts from 00:00 to 30:00(wil never be this much)
            lateTimer = new CountUpTimer(30 * 60 * 1000, 1000, description);
            lateTimer.start();
        }

        synchronized boolean isRunning() {
            return isRunning;
        }
    }

    private class CountUpTimer extends CountDownTimer {

        private String description;
        private long totalMillis;
        private boolean isRunning;

        CountUpTimer(long millisInFuture, long countDownInterval, String description) {
            super(millisInFuture, countDownInterval);
            this.description = description;
            this.totalMillis = millisInFuture;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            isRunning = true;
            long elapsedTimeMillis = totalMillis - millisUntilFinished;
            long elapsedTimeSeconds = elapsedTimeMillis / 1000;
            long minutes = elapsedTimeSeconds / 60;
            long seconds = elapsedTimeSeconds % 60;
            timerTextView.setText(String.format(Locale.getDefault(), "%s +%02d:%02d", description, minutes, seconds));
        }

        @Override
        public void onFinish() {
            isRunning = false;
        }

        boolean isRunning() {
            return isRunning;
        }
    }
}
