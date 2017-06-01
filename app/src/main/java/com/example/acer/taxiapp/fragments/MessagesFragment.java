package com.example.acer.taxiapp.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.acer.taxiapp.R;

import java.util.List;

public class MessagesFragment extends Fragment {

    private ListView messagesList;
    private List<String> messages;
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
        Log.e("MSG_FRAGMENT", "On Create View");
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        messagesList = (ListView) view.findViewById(R.id.list_view_messages);
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

    private class MessageListAdapter extends ArrayAdapter<String> {

        private Context context;
        private List<String> messages;

        MessageListAdapter(@NonNull Context context, @NonNull List<String> objects) {
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
            TextView messageTextView = (TextView) view.findViewById(R.id.text_view_list_item_message_text);
            TextView timeTextView = (TextView) view.findViewById(R.id.text_view_list_item_message_time);

            String message = getItem(position);
            if(message != null) {
                messageTextView.setText(message.substring(0, message.length() - 8));
                timeTextView.setText(message.substring(message.length() - 8, message.length()));
            }
            return view;
        }
    }

    public interface MessageListProvider {
        List<String> getMessages();
    }
}
