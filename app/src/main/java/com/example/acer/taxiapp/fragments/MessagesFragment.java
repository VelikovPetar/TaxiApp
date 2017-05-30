package com.example.acer.taxiapp.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.acer.taxiapp.R;

import java.util.List;

/**
 * Created by Acer on 30.5.2017.
 */

public class MessagesFragment extends ListFragment {

    public static final String MESSAGES = "messagesfragment.messages";
    private List<String> messages;
    private MessageListAdapter mla;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        messages = args.getStringArrayList(MESSAGES);
        mla = new MessageListAdapter(getActivity(), messages);
        setListAdapter(mla);
    }

    public void notifyDataSetChanged() {
        mla.notifyDataSetChanged();
    }

    private class MessageListAdapter extends ArrayAdapter<String> {

        private Context context;
        private List<String> messages;

        public MessageListAdapter(@NonNull Context context, @NonNull List<String> objects) {
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
}
