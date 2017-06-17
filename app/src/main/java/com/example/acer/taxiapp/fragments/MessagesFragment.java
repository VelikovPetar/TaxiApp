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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.MessageListProvider;
import com.example.acer.taxiapp.PopupMessage;
import com.example.acer.taxiapp.R;

import java.util.List;

public class MessagesFragment extends Fragment {

    private ListView messagesList;
    private List<PopupMessage> messages;
    private MessageListAdapter adapter;

    // Reference to MainActivity for getting the list of messages
    private MessageListProvider provider;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e("MSG_FRAGMENT", "On Attach(context)");
        try {
            provider = (MessageListProvider) context;
        } catch(ClassCastException e) {
            e.printStackTrace();
            Log.e("MSG_FRAGMENT", "Class Cast Exception");
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
            Log.e("MSG_FRAGMENT", "On Attach(activity)");
            try {
                provider = (MessageListProvider) activity;
            } catch (ClassCastException e) {
                e.printStackTrace();
                Log.e("MSG_FRAGMENT", "Class Cast Exception");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        messagesList = (ListView) view.findViewById(R.id.list_view_messages);
        messagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // TODO Vo header izvor
                PopupMessage popupMessage = adapter.getItem(position);
                String fullMessage;
                if(popupMessage != null) {
                    byte source = popupMessage.getMessageSource();
                    builder.setTitle(String.format("%s(%s)", getString(R.string.full_message), source == '0' ? getString(R.string.dispatcher) :
                            (source == '3' ? getString(R.string.android) : getString(R.string.system))));
                    fullMessage = popupMessage.getTextMessage();
                } else {
                    builder.setTitle(R.string.full_message);
                    fullMessage = getString(R.string.error_reading_full_message);
                }
                builder.setMessage(fullMessage);
                builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        messages = provider.getMessages();
        adapter = new MessageListAdapter(getActivity(), messages);
        messagesList.setAdapter(adapter);
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    private class MessageListAdapter extends ArrayAdapter<PopupMessage> {

        private Context context;
        private List<PopupMessage> messages;

        MessageListAdapter(@NonNull Context context, @NonNull List<PopupMessage> objects) {
            super(context, 0, objects);
            this.context = context;
            messages = objects;
        }

        @Override
        public int getCount() {
            return messages.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if(view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.list_item_message, parent, false);
            }
            TextView sourceTextView = (TextView) view.findViewById(R.id.text_view_list_item_message_source);
            TextView messageTextView = (TextView) view.findViewById(R.id.text_view_list_item_message_text);
            TextView timeTextView = (TextView) view.findViewById(R.id.text_view_list_item_message_time);

            PopupMessage popupMessage = getItem(position);
            if(popupMessage != null) {
                byte source = popupMessage.getMessageSource();
                sourceTextView.setText(source == '0' ? R.string.dispatcher : (source == '3' ? R.string.android : R.string.system));
                String textMessage = popupMessage.getTextMessage();
                if(textMessage.length() > 30) {
                    messageTextView.setText(String.format("%s...", textMessage.substring(0, 30)));
                } else {
                    messageTextView.setText(textMessage);
                }
                timeTextView.setText(popupMessage.getTimestamp());
            }
            return view;
        }
    }

}
