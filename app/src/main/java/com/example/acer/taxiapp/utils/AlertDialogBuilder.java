package com.example.acer.taxiapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.tcp.TCPClient;

public class AlertDialogBuilder {
    public static final int REGISTER_FOR_REGION_ALERT_DIALOG = 1;
    public static final int STATUS_REQUEST_ALERT_DIALOG = 2;

    public static AlertDialog build(Context context, int alertDialogType) {
        AlertDialog dialog = null;
        switch(alertDialogType) {
            case REGISTER_FOR_REGION_ALERT_DIALOG:
                dialog = createRegisterForRegionAlertDialog(context);
                break;
            case STATUS_REQUEST_ALERT_DIALOG:
                dialog = createStatusRequestAlertDialog(context);
                break;
        }
        return dialog;
    }

    public static AlertDialog build(Context context, boolean shouldChooseDestination) {
        return createEnterGeneratedMessageDialog(context, shouldChooseDestination);
    }

    public static AlertDialog build(Context context, String text, int priority) {
        return createConfirmGeneratedMessageDialog(context, text, priority);
    }

    private static AlertDialog createStatusRequestAlertDialog(final Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_status_request, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogLayout)
                .setPositiveButton(R.string.send, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface _dialog) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText editText = (EditText) dialogLayout.findViewById(R.id.edit_text_status_request);
                        String text = editText.getText().toString().trim();
                        if(text.equals("")) {
                            editText.setHint(R.string.error_enter_text);
                            editText.setHintTextColor(Color.RED);
                            return;
                        }
                        byte[] message = MessengerClient.getRequestStatusMessage(text, context);
                        TCPClient tcpClient = TCPClient.getInstance(context);
                        if(!tcpClient.sendBytes(message)) {
                            Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private static AlertDialog createRegisterForRegionAlertDialog(final Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_register_for_region, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogLayout)
                .setPositiveButton(R.string.send, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
                        EditText regionEditText = (EditText) dialogLayout.findViewById(R.id.edit_text_register_for_region);
                        String regionText = regionEditText.getText().toString().trim();
                        if(regionText.equals("")) {
                            regionEditText.setHint(R.string.error_enter_region);
                            regionEditText.setHintTextColor(Color.RED);
                            return;
                        }
                        int region = Integer.parseInt(regionText.trim());
                        byte[] message = MessengerClient.getRegisterForRegionMessage(region, context);
                        TCPClient tcpClient = TCPClient.getInstance(context);
                        if(!tcpClient.sendBytes(message)) {
                            Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private static AlertDialog createEnterGeneratedMessageDialog(final Context context, final boolean shouldChooseDestination) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_enter_generated_message, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogLayout)
                .setPositiveButton(R.string.send, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface _dialog) {
                final TextView msgDestination = (TextView) dialogLayout.findViewById(R.id.text_view_message_destination);
                RadioGroup destinationRadioGroup = (RadioGroup) dialogLayout.findViewById(R.id.radio_group_choose_destination);
                destinationRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            case R.id.radio_button_destination_android:
                                msgDestination.setText(R.string.dest_android);
                                break;
                            case R.id.radio_button_destination_dispatcher:
                                msgDestination.setText(R.string.dest_dispatcher);
                                break;
                        }
                    }
                });
                if(shouldChooseDestination) {
                    msgDestination.setText(R.string.dest_android);
                    destinationRadioGroup.setVisibility(View.VISIBLE);
                } else {
                    msgDestination.setText(R.string.dest_dispatcher);
                    destinationRadioGroup.setVisibility(View.GONE);
                }
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
                        byte destination = '0';
                        RadioGroup destinationRadioGroup = (RadioGroup) dialogLayout.findViewById(R.id.radio_group_choose_destination);
                        if(shouldChooseDestination) {
                            int choice = destinationRadioGroup.getCheckedRadioButtonId();
                            switch (choice) {
                                case R.id.radio_button_destination_android:
                                    destination = '3';
                                    break;
                                case R.id.radio_button_destination_dispatcher:
                                    destination = '0';
                                    break;
                            }
                        }
                        byte[] message = MessengerClient.getGeneratedMessage(destination, '7', text, context);
                        TCPClient tcpClient = TCPClient.getInstance(context);
                        if(!tcpClient.sendBytes(message)) {
                            Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private static AlertDialog createConfirmGeneratedMessageDialog(final Context context, final String text, final int priority) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View dialogLayout = layoutInflater.inflate(R.layout.dialog_confirm_generated_message, null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogLayout)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        byte[] message = MessengerClient.getGeneratedMessage((byte)'0', Character.forDigit(priority + 1, 10), text, context);
                        TCPClient tcpClient = TCPClient.getInstance(context);
                        if(!tcpClient.sendBytes(message)) {
                            Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        TextView msgDestination = (TextView) dialogLayout.findViewById(R.id.text_view_message_to);
        msgDestination.setText(R.string.dest_dispatcher);
        TextView msgContent = (TextView) dialogLayout.findViewById(R.id.text_view_generated_message);
        msgContent.setText(String.format("%s %s", context.getString(R.string.message), text));
        return dialog;
    }
}
