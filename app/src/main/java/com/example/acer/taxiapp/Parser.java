package com.example.acer.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.fragments.StatusBarFragment;
import com.example.acer.taxiapp.services.TCPClientIntentService;

import java.util.Arrays;


public class Parser {

    // Debug
    private static final String DEBUG_TAG = "BYTE-PARSER";

    // Name for broadcasts concerning status bar updates
    public static final String VALUE = "status_bar_update_value";
    public static final String COLOR = "status_bar_update_color";

    private Context context;
    private String deviceId;

    public Parser(Context context) {
        this.context = context;
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        deviceId = preferences.getString(MainActivity.DEVICE_ID, null);
    }

    public void parse(byte[] message) {
//        for(byte b : message) {
//            Log.e(DEBUG_TAG, b + "\t" + (char)b);
//        }
        // Check if it is OK message ot heartbeat message
        if(message.length <= 2) {
            // OK <- successful connection
            // or
            // Z <- heartbeat
        } else {
            // Check if it is Confirmation message(BB) or incoming message(AA)
            if(message[0] == 'A' && message[1] == 'A') {
                Log.e(DEBUG_TAG, bytesToString(message) + " : Received from server.");

                // Process it

                // Check if the message is for this device
                if(!confirmVehicleId(message)) {
                    Log.e(DEBUG_TAG, "Wrong DeviceId!");
                    return;
                }

                // Get the type of the command
                String command;
                if(message.length >= 9) {
                    command = (char) message[7] + "" + (char) message[8];
                } else {
                    Log.e(DEBUG_TAG, "Full message not received");
                    command = "";
                }
                switch(command) {
                    case "45":
                        Log.e(DEBUG_TAG, "Popup message.");
                        parsePopupMessage(message);
                        break;
                }

            } else if(message[0] == 'B' && message[1] == 'B') {
                // Confirmation of received message
                Log.e(DEBUG_TAG, bytesToString(message) + " : Confirmation-Server received the message.");
            }
        }
    }

    private void parsePopupMessage(byte[] message) {

        // Calculate the length of the message text
        int lengthOfMessage = 0;
        lengthOfMessage += (message[9] - '0') * 100;
        lengthOfMessage += (message[10] - '0') * 10;
        lengthOfMessage += (message[11] - '0');

        // TODO Check message validity
        // message[12] =!?

        // AAxxxxxyyccc(12) +
        // Validnost(1) + length(text) + source(1) // lengthOfMessage +
        // checksum(2) + zzzzz(5)
        if(message.length != 12 + lengthOfMessage + 2 + 5) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }

        // Check if it is confirmation of successful login - SPECIAL CASE
        if(message[13] == '1' && message[14] == '1' && message[15] == '1') {
            // Confirmation of successful login
            // Display the name of the driver on the status bar
            byte[] driverNameBytes = Arrays.copyOfRange(message, 16, 12 + lengthOfMessage - 1);
            String driverName = bytesToString(driverNameBytes);
            broadcastStatusUpdate(TCPClientIntentService.BroadcastActions.ACTION_DRIVER_STATUS, new StatusBarFragment.DriverStatusValue(driverName, Color.GREEN));
        } else {
            // Regular popup message
            byte[] popupMessageTextBytes = Arrays.copyOfRange(message, 13, 12 + lengthOfMessage - 1);
            String popupMessageText = bytesToString(popupMessageTextBytes);

            // Check for the message source
            byte source = message[12 + lengthOfMessage - 1];
            switch(source) {
                case '4': // System
                    // Don't display
                    Log.e(DEBUG_TAG, "System message");
                    break;
                case '0': // Dispatcher
                    Log.e(DEBUG_TAG, "Dispatcher message.");
                    // TODO Broadcast
                    popupMessageText = "Диспечер: " + popupMessageText;
                    break;
                case '3': // Android
                    Log.e(DEBUG_TAG, "Android message");
                    // TODO Broadcast
                    popupMessageText = "Android: " + popupMessageText;
                    break;
            }
        }
    }

    private String bytesToString(byte[] bytes) {
        String ret = "";
        for(byte b : bytes) {
            ret += (char) b;
        }
        return ret;
    }

    private boolean confirmVehicleId(byte[] message) {
        if(message.length < 7) {
            Log.e(DEBUG_TAG, "Full message not received");
            return false;
        }
        byte[] deviceIdBytes = Arrays.copyOfRange(message, 2, 7);
        return deviceId.equals(bytesToString(deviceIdBytes));
    }

    private void broadcastStatusUpdate(String action, StatusBarFragment.StatusUpdate statusUpdate) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(VALUE, statusUpdate.getValue());
        intent.putExtra(COLOR, statusUpdate.getColor());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
