package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.utils.AlertDialogBuilder;
import com.example.acer.taxiapp.MessageListProvider;
import com.example.acer.taxiapp.models.PopupMessage;
import com.example.acer.taxiapp.R;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GeneratedMessagesFragment extends Fragment {

    private ListView messageCategoriesList;
    private ListView messageItemsList;

    private OnCategoryClickListener onCategoryClickListener = new OnCategoryClickListener();

    private ArrayAdapter<String> messageCategoriesAdapter;
    private ArrayAdapter<String> messageItemsAdapter;

    private String[] categoriesNames;
    private String[] categoryItems;

    // Reference to MainActivity for getting the list of messages
    private MessageListProvider provider;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            provider = (MessageListProvider) context;
        } catch(ClassCastException e) {
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
                provider = (MessageListProvider) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generated_messages, container, false);
        messageCategoriesList = (ListView) view.findViewById(R.id.list_view_generated_messages_categories);
        messageItemsList = (ListView) view.findViewById(R.id.list_view_generated_messages_items);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        categoriesNames = getResources().getStringArray(R.array.categories);

        // Adapter for the categories
        messageCategoriesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, categoriesNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                if(view == null) {
                    view = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(categoriesNames[position]);
                if(position == onCategoryClickListener.getLastCategorySelected()) {
                    view.setBackgroundColor(Color.DKGRAY);
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                return view;
            }
        };
        messageCategoriesList.setAdapter(messageCategoriesAdapter);
        messageCategoriesList.setOnItemClickListener(onCategoryClickListener);

        // Action to do when clicking on a generated mesage
        messageItemsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showConfirmMessageDialog(categoryItems[position], onCategoryClickListener.getLastCategorySelected());
            }
        });
    }

    private void showConfirmMessageDialog(final String text, final int priority) {
        AlertDialog dialog = AlertDialogBuilder.build(getActivity(), text, priority);
        dialog.show();
    }

    private void showEnterMessageDialog() {
        final boolean shouldChooseDestination = shouldChooseDestination();
        AlertDialog dialog = AlertDialogBuilder.build(getActivity(), shouldChooseDestination);
        dialog.show();
    }

    private boolean shouldChooseDestination() {
        List<PopupMessage> messages = provider.getMessages();
        if(messages.size() < 1)
            return false;
        PopupMessage lastMessage = messages.get(0);
        String lastMessageTime = lastMessage.getTimestamp();
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String currentTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        int differenceInSeconds = differenceInSeconds(lastMessageTime, currentTime);
        if(differenceInSeconds > 3 * 60)
            return false;
        if(lastMessage.getMessageSource() == '3')
            return true;
        return false;
    }

    private int differenceInSeconds(String time1, String time2) {
        String[] part = time1.split(":");
        int inSeconds1 = Integer.parseInt(part[0]) * 60 * 60
                + Integer.parseInt(part[1]) * 60
                +Integer.parseInt(part[2]);
        part = time2.split(":");
        int inSeconds2 = Integer.parseInt(part[0]) * 60 * 60
                + Integer.parseInt(part[1]) * 60
                +Integer.parseInt(part[2]);
        if(inSeconds2 < inSeconds1) {
            inSeconds2 += 86400;
        }
        return inSeconds2 - inSeconds1;
    }

    private class OnCategoryClickListener implements AdapterView.OnItemClickListener {

        private int lastCategorySelected = -1;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    categoryItems = getResources().getStringArray(R.array.pomosh_za_povik);
                    break;
                case 1:
                    categoryItems = getResources().getStringArray(R.array.servis);
                    break;
                case 2:
                    categoryItems = getResources().getStringArray(R.array.povik);
                    break;
                case 3:
                    categoryItems = getResources().getStringArray(R.array.sostojba);
                    break;
                case 4:
                    categoryItems = getResources().getStringArray(R.array.interventni);
                    break;
                case 5:
                    categoryItems = getResources().getStringArray(R.array.informativni);
                    break;
                case 6:
                    categoryItems = new String[0];
                    showEnterMessageDialog();
                    break;
            }
            messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
            messageItemsList.setAdapter(messageItemsAdapter);

            if(lastCategorySelected != -1 && lastCategorySelected != position) {
                int firstVisibleRow = messageCategoriesList.getFirstVisiblePosition();
                int lastVisibleRow = messageCategoriesList.getLastVisiblePosition();

                if(lastCategorySelected >= firstVisibleRow && lastCategorySelected <= lastVisibleRow) {
                    int actualPosition = lastCategorySelected - firstVisibleRow;
                    View lastRowSelected = messageCategoriesList.getChildAt(actualPosition);
                    lastRowSelected.setBackgroundColor(Color.TRANSPARENT);
                }
            }
            view.setBackgroundColor(Color.DKGRAY);
            lastCategorySelected = position;
        }

        int getLastCategorySelected() {
            return lastCategorySelected;
        }
    }
}
