package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.MessageListProvider;
import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.TCPClient;

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
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_confirm_generated_message, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(dialogLayout)
                .setPositiveButton("Испрати", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("DIALOGS", priority + "");
                        byte[] message = MessengerClient.getGeneratedMessage((byte)'0', Character.forDigit(priority + 1, 10), text, getActivity());
//                        TCPClient tcpClient = TCPClient.getInstance(getActivity());
//                        tcpClient.sendBytes(message);
                        String msg = "";
                        for(byte b : message)
                            msg += (char) b;
                        Log.e("DIALOGS", msg);
                    }
                })
                .setNegativeButton("Откажи", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        TextView msgDestination = (TextView) dialogLayout.findViewById(R.id.text_view_message_to);
        msgDestination.setText("До: Диспечер");
        TextView msgContent = (TextView) dialogLayout.findViewById(R.id.text_view_generated_message);
        msgContent.setText("Порака: " + text);
        dialog.show();
    }

    private void showEnterMessageDialog() {
        final byte destination = calculateDestination();
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_enter_generated_message, null);
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
                        EditText messageEditText = (EditText) dialogLayout.findViewById(R.id.edit_text_generated_message);
                        String text = messageEditText.getText().toString().trim();
                        if(text.equals("")) {
                            messageEditText.setHint(R.string.error_enter_text);
                            messageEditText.setHintTextColor(Color.RED);
                            return;
                        }
                        byte[] message = MessengerClient.getGeneratedMessage(destination, '7', text, getActivity());
//                        TCPClient tcpClient = TCPClient.getInstance(getActivity());
//                        tcpClient.sendBytes(message);
                        String msg = "";
                        for(byte b : message)
                            msg += (char) b;
                        Log.e("DIALOGS", msg);
                        dialog.dismiss();
                    }
                });
            }
        });
        TextView msgDestination = (TextView) dialogLayout.findViewById(R.id.text_view_message_destination);
        msgDestination.setText(destination == '0' ? "До: Диспечер" : "До: Android");

        dialog.show();
    }

    private byte calculateDestination() {
        List<String> messages = provider.getMessages();
        if(messages.size() < 1)
            return '0';
        String lastMessage = messages.get(0);
        String lastMessageTime = lastMessage.substring(lastMessage.length() - 8, lastMessage.length());
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String currentTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        int differenceInSeconds = differenceInSeconds(lastMessageTime, currentTime);
        if(differenceInSeconds > 3 * 60)
            return '0';
        if(lastMessage.startsWith("Android"))
            return '3';
        return '0';
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
        Log.e("DIALOGS", (inSeconds2-inSeconds1) +"");
        return inSeconds2 - inSeconds1;
    }

    private class OnCategoryClickListener implements AdapterView.OnItemClickListener {

        private int lastCategorySelected = -1;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    categoryItems = getResources().getStringArray(R.array.pomosh_za_povik);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 1:
                    categoryItems = getResources().getStringArray(R.array.servis);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 2:
                    categoryItems = getResources().getStringArray(R.array.povik);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 3:
                    categoryItems = getResources().getStringArray(R.array.sostojba);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 4:
                    categoryItems = getResources().getStringArray(R.array.interventni);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 5:
                    categoryItems = getResources().getStringArray(R.array.informativni);
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    break;
                case 6:
                    categoryItems = new String[0];
                    messageItemsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, categoryItems);
                    messageItemsList.setAdapter(messageItemsAdapter);
                    showEnterMessageDialog();
                    break;
            }
            if(lastCategorySelected != -1 && lastCategorySelected != position) {
                int firstVisibleRow = messageCategoriesList.getFirstVisiblePosition();
                int lastVisibleRow= messageCategoriesList.getLastVisiblePosition();

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
