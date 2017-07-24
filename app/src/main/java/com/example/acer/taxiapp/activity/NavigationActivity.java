package com.example.acer.taxiapp.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.fragments.ButtonListFragment;
import com.example.acer.taxiapp.fragments.CheckCardFragment;
import com.example.acer.taxiapp.fragments.ConfigFragment;
import com.example.acer.taxiapp.fragments.GeneratedMessagesFragment;
import com.example.acer.taxiapp.fragments.LoginFragment;
import com.example.acer.taxiapp.fragments.MessagesFragment;
import com.example.acer.taxiapp.fragments.OffersFragment;
import com.example.acer.taxiapp.fragments.OffersStatusBarFragment;
import com.example.acer.taxiapp.fragments.StatusBarFragment;
import com.example.acer.taxiapp.models.LongOffer;
import com.example.acer.taxiapp.models.ShortOffer;
import com.example.acer.taxiapp.utils.Utils;

import java.util.ArrayList;

/**
 * Activity that handles setup of UI,
 * restoring the state, and navigation between fragments.
 */
public class NavigationActivity extends Activity {

    // Indicator whether the driver is logged in
    // Stored here because it used in restricting the access to certain functions
    protected boolean isLoggedIn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize fragments
        final FragmentManager fManager = getFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.add(R.id.fragment_offers_container, new OffersStatusBarFragment(), "TAG_OFFERS_STATUS_BAR_FRAGMENT");
            fTransaction.add(R.id.fragment_buttons_container, new ButtonListFragment(), "TAG_BUTTONS_LIST_FRAGMENT");
            fTransaction.add(R.id.fragment_status_bar_container, new StatusBarFragment(), "TAG_STATUS_BAR_FRAGMENT");
            fTransaction.add(R.id.fragment_content_container, new LoginFragment(), "TAG_LOGIN_FRAGMENT");
            fTransaction.commit();
        }

        // Special case: On some android devices, when pressing the power button(off then on),
        // the activity changes orientation effectively destroying itself(with that it send message for logging out).
        // When the activity is recreated, it restores the state of the other fragments, but it remains in not logged state
        // So when that happens, we need to clear the fragments from the stack before the activity was destroyed, and add the Login fragment
        if(savedInstanceState != null && !isLoggedIn) {
            while(fManager.getBackStackEntryCount() > 0)
                fManager.popBackStackImmediate();
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.replace(R.id.fragment_content_container, new LoginFragment(), "TAG_LOGIN_FRAGMENT");
            fTransaction.commit();
        }

        ImageButton configButton = (ImageButton) findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigFragment configFragment = (ConfigFragment) fManager.findFragmentByTag("TAG_CONFIG_FRAGMENT");
                if(Utils.isFragmentVisible(configFragment)) {
                    return;
                }
                FragmentTransaction fTransaction = fManager.beginTransaction();
                fTransaction.replace(R.id.fragment_content_container, new ConfigFragment(), "TAG_CONFIG_FRAGMENT");
                fTransaction.addToBackStack("frag_conf");
                fTransaction.commit();
            }
        });
    }

    protected void updateMessagesFragments(int messagesCount, boolean wasItemAdded) {
        // If the message fragment is visible, update the list view displaying the messages
        FragmentManager fManager = getFragmentManager();
        MessagesFragment messagesFragment = (MessagesFragment) fManager.findFragmentByTag("TAG_POPUP_MESSAGES_FRAGMENT");
        if(Utils.isFragmentVisible(messagesFragment)) {
            messagesFragment.notifyDataSetChanged();
        }

        // Update the offers status bar
        OffersStatusBarFragment offersStatusBarFragment =
                (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
            offersStatusBarFragment.setMessagesCount(messagesCount, wasItemAdded);
        }
    }

    protected void updateOfferFragments(LongOffer longOffer, ArrayList<ShortOffer> shortOffers) {
        FragmentManager fManager = getFragmentManager();
        // If the offers fragment is visible, update the list view displaying the short offers
        OffersFragment offersFragment = (OffersFragment) fManager.findFragmentByTag("TAG_OFFERS_FRAGMENT");
        if(Utils.isFragmentVisible(offersFragment)) {
            offersFragment.notifyDataSetChanged();
            if(longOffer != null) {
                offersFragment.displayLongOffer(longOffer);
            } else {
                offersFragment.hideLongOffer();
            }
        }
        // Update the offers status bar
        OffersStatusBarFragment offersStatusBarFragment =
                (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
            if(longOffer != null) {
                offersStatusBarFragment.setHasLongOffer(true);
            } else {
                int read = 0;
                for(ShortOffer so : shortOffers) {
                    read += so.isRead() ? 1 : 0;
                }
                offersStatusBarFragment.setHasLongOffer(false);
                offersStatusBarFragment.setUnreadOffers(shortOffers.size() - read);
                offersStatusBarFragment.setTotalOffers(shortOffers.size());
            }
        }
    }

    @Override
    public void onBackPressed() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.exit)
                .setMessage(R.string.confirm_exit)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NavigationActivity.super.onBackPressed();
                        }
                    })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                .create();
        dialog.show();
    }

    public void onLoginButtonClick(Location lastLocation) {
        if(!isLoggedIn) {
            FragmentManager fManager = getFragmentManager();
            boolean isPopped = fManager.popBackStackImmediate("frag_conf", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (!isPopped) {
                LoginFragment loginFragment = (LoginFragment) fManager.findFragmentByTag("TAG_LOGIN_FRAGMENT");
                if (!Utils.isFragmentVisible(loginFragment)) {
                    FragmentTransaction fTransaction = fManager.beginTransaction();
                    LoginFragment newLoginFragment = new LoginFragment();
                    if(lastLocation != null)
                        newLoginFragment.initLocation(lastLocation);
                    fTransaction.replace(R.id.fragment_content_container, newLoginFragment, "TAG_LOGIN_FRAGMENT");
                    fTransaction.addToBackStack(null);
                    fTransaction.commit();
                }
            }
        }

    }

    public void onMapButtonClick() {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        // If the driver is logged in, the map is always the bottom fragment
        FragmentManager fManager = getFragmentManager();
        while(fManager.getBackStackEntryCount() > 0)
            fManager.popBackStackImmediate();
    }

    public void showMessagesFragment(int messagesCount) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
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
        // Update the offers status bar
        OffersStatusBarFragment offersStatusBarFragment =
                (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
            offersStatusBarFragment.setMessagesCount(messagesCount, false);
        }
    }

    public void showOffersFragment(LongOffer longOffer, ArrayList<ShortOffer> shortOffers) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
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
        // Update the offers status bar
        OffersStatusBarFragment offersStatusBarFragment =
                (OffersStatusBarFragment) fManager.findFragmentByTag("TAG_OFFERS_STATUS_BAR_FRAGMENT");
        if(Utils.isFragmentVisible(offersStatusBarFragment)) {
            if(longOffer == null) {
                offersStatusBarFragment.setUnreadOffers(0);
                offersStatusBarFragment.setTotalOffers(shortOffers.size());
            }
        }
    }

    public void showGeneratedMessagesFragment() {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        FragmentManager fManager = getFragmentManager();
        GeneratedMessagesFragment generatedMessagesFragment =
                (GeneratedMessagesFragment) fManager.findFragmentByTag("TAG_GENERATED_MESSAGES_FRAGMENT");
        if(Utils.isFragmentVisible(generatedMessagesFragment))
            return;
        boolean popped = fManager.popBackStackImmediate("generated_messages_frag", 0);
        if(!popped) {
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.replace(R.id.fragment_content_container, new GeneratedMessagesFragment(), "TAG_GENERATED_MESSAGES_FRAGMENT");
            fTransaction.addToBackStack("generated_messages_frag");
            fTransaction.commit();
        }
    }

    public void showCheckCardFragment(Location lastLocation) {
        if(!isLoggedIn) {
            Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_LONG).show();
            return;
        }
        FragmentManager fManager = getFragmentManager();
        CheckCardFragment checkCardFragment =
                (CheckCardFragment) fManager.findFragmentByTag("TAG_CHECK_CARD_FRAGMENT");
        if(Utils.isFragmentVisible(checkCardFragment)) {
            return;
        }
        boolean popped = fManager.popBackStackImmediate("check_card_frag", 0);
        if(!popped) {
            FragmentTransaction fTransaction = fManager.beginTransaction();
            fTransaction.replace(R.id.fragment_content_container, new CheckCardFragment(), "TAG_CHECK_CARD_FRAGMENT");
            fTransaction.addToBackStack("check_card_frag");
            fTransaction.commit();
        }
    }
}
