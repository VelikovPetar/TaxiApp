package com.example.acer.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.fragments.StatusBarFragment;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Parser {

    // Debug
    private static final String DEBUG_TAG = "BYTE-PARSER";

    // Name for the messages that are broadcast by the TcpClient
    public static final String MESSAGE = "broadcast_message";

    // Name for broadcasts concerning global vehicle state updates
    public static final String VEHICLE_STATE = "vehicle_state";

    // Names for broadcasts concerning status bar updates
    public static final String VALUE = "status_bar_update_value";
    public static final String COLOR = "status_bar_update_color";

    // Names for extras for offers broadcasts
    public static final String ID_PHONE_CALL = "id_phone_call_extra";
    public static final String LATITUDE = "latitude_extra";
    public static final String LONGITUDE = "longitude_extra";
    public static final String OFFER_SOURCE = "offer_source_extra";
    public static final String TEXT_MESSAGE = "text_message_extra";

    private Context context;
    private String deviceId;

    // Constants
    private static final int HEADER_LENGTH = 9;
    private static final int CHECKSUM_LENGTH = 2;
    private static final int PADDING_LENGTH = 5;

    public Parser(Context context) {
        this.context = context;
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        deviceId = preferences.getString(MainActivity.DEVICE_ID, null);
    }

    public void parse(byte[] message) {
//        for(byte b : message) {
//            Log.e(DEBUG_TAG, b + "\t" + (char)b);
//        }
        // Check if it is OK message or heartbeat message
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
                    case "34":
                        Log.e(DEBUG_TAG, "Kratka najava.");
                        parseShortOffer(message);
                        break;
                    case "35":
                        Log.e(DEBUG_TAG, "Brishenje na kratka najava.");
                        parseCancelShortOffer(message);
                        break;
                    case "36":
                        Log.e(DEBUG_TAG, "Dolga najava.");
                        parseLongOffer(message);
                        break;
                    case "40":
                        Log.e(DEBUG_TAG, "Status update message.");
                        parseStatusUpdateMessage(message);
                        break;
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
        if(message.length != HEADER_LENGTH + 3 + lengthOfMessage + CHECKSUM_LENGTH + PADDING_LENGTH) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }

        // Check if it is confirmation of successful login - SPECIAL CASE
        if(message[13] == '1' && message[14] == '1' && message[15] == '1') {
            // Confirmation of successful login
            // Display the name of the driver on the status bar
            byte[] driverNameBytes = Arrays.copyOfRange(message, 16, 12 + lengthOfMessage - 1);
            String driverName = bytesToString(driverNameBytes);
            broadcastStatusUpdate(BroadcastActions.ACTION_DRIVER_STATUS, new StatusBarFragment.DriverStatusValue(driverName, Color.GREEN));
        // Check if it is confirmation of successful logout - SPECIAL CASE TODO Mozhebi poinaku kje se spravuvame
        } else if(message[13] == '0' && message[14] == '0' && message[15] == '0') {
            broadcastStatusUpdate(BroadcastActions.ACTION_DRIVER_STATUS, new StatusBarFragment.DriverStatusValue("Нема најавен возач", Color.YELLOW));
        } else {
            // Regular popup message
            byte[] popupMessageTextBytes = Arrays.copyOfRange(message, 13, 12 + lengthOfMessage - 1);
            String popupMessageText = bytesToString(popupMessageTextBytes);

            // Append time to the message text
            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String timeStamp = String.format(Locale.getDefault(), "%02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            // Check for the message source
            byte source = message[12 + lengthOfMessage - 1];
            switch(source) {
                case '4': // System
                    // Don't display
                    // TODO Kako da go prikazheme?
                    Log.e(DEBUG_TAG, "System message");
                    popupMessageText = "Систем: " + popupMessageText + timeStamp;
                    broadcastMessage(BroadcastActions.ACTION_POPUP_MESSAGE, popupMessageText);
                    break;
                case '0': // Dispatcher
                    Log.e(DEBUG_TAG, "Dispatcher message.");
                    popupMessageText = "Диспечер: " + popupMessageText + timeStamp;
                    broadcastMessage(BroadcastActions.ACTION_POPUP_MESSAGE, popupMessageText);
                    break;
                case '3': // Android
                    Log.e(DEBUG_TAG, "Android message");
                    popupMessageText = "Android: " + popupMessageText + timeStamp;
                    broadcastMessage(BroadcastActions.ACTION_POPUP_MESSAGE, popupMessageText);
                    break;
            }
        }
    }

    private void parseStatusUpdateMessage(byte[] message) {
        final int stateLength = 20;

        // AAxxxxxyy(9)
        int startPos = 9;
        if(message.length < HEADER_LENGTH + stateLength + 2 + 1 + 1 + CHECKSUM_LENGTH + PADDING_LENGTH) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }
        String newState = "";
        for(int i = startPos; i < startPos + stateLength; ++i) {
            if(message[i] == 0)
                break;
            newState += (char) message[i];
        }

        // SEGA ZA SEGA NE GI KORISTIME OVIE
        // Vreme vo sostojba?
        // message[startPos + stateLength] ?!
        // message[startPos + stateLength + 1] ?!

        // Prodolzhuvanje na vreme?
        // message[startPos + stateLength + 2] ?!

        // Sostojba?
        // message[startPos + stateLength + 3] ?!
        broadcastVehicleStateUpdate((int) message[startPos + stateLength + 3]);

        // Show status on the Status bar
        broadcastStatusUpdate(BroadcastActions.ACTION_VEHICLE_STATE_STATUS, new StatusBarFragment.VehicleStatusValue(newState.replace("State", ""), Color.CYAN));

        Log.e(DEBUG_TAG, "STATE = " + newState);
    }

    private void parseShortOffer(byte[] message) {
        final int idPhoneCallLength = 4;
        final int offerSourceLength = 1;
        final int textMessageLength = 60;
        if(message.length < HEADER_LENGTH + idPhoneCallLength + offerSourceLength + textMessageLength + CHECKSUM_LENGTH + PADDING_LENGTH) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }
//        byte[] tmp = new byte[idPhoneCallLength];
//        for(int i = 0; i < idPhoneCallLength; ++i) {
//            tmp[idPhoneCallLength - i - 1] = message[HEADER_LENGTH + i];
//        }
//        long idPhoneCall = ByteBuffer.wrap(tmp).getLong();
        byte[] tmp = Arrays.copyOfRange(message, HEADER_LENGTH, HEADER_LENGTH + idPhoneCallLength);

        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
        Log.e(DEBUG_TAG, "ID phonecall = " + idPhoneCall);
        byte offerSource = message[HEADER_LENGTH + idPhoneCallLength];
        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength + offerSourceLength, HEADER_LENGTH + idPhoneCallLength + offerSourceLength + textMessageLength);
        String textMessage = bytesToString(tmp).trim();
        broadcastOffer(BroadcastActions.ACTION_SHORT_OFFER, idPhoneCall, offerSource, textMessage);
    }

    private void parseCancelShortOffer(byte[] message) {
        final int idPhoneCallLength = 4;
        final int textMessageLength = 60;
        if(message.length < HEADER_LENGTH + idPhoneCallLength + textMessageLength + CHECKSUM_LENGTH + PADDING_LENGTH) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }
//        byte[] tmp = new byte[idPhoneCallLength];
//        for(int i = 0; i < idPhoneCallLength; ++i) {
//            tmp[idPhoneCallLength - i - 1] = message[HEADER_LENGTH + i];
//        }
//        long idPhoneCall = ByteBuffer.wrap(tmp).getLong();
        byte[] tmp = Arrays.copyOfRange(message, HEADER_LENGTH, HEADER_LENGTH + idPhoneCallLength);
        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt(); // Int because 4 bytes

        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength, HEADER_LENGTH + idPhoneCallLength + textMessageLength);
        String textMessage = bytesToString(tmp).trim();
        broadcastOffer(BroadcastActions.ACTION_CANCEL_SHORT_OFFER, idPhoneCall, (byte) -1, textMessage);
    }

    private void parseLongOffer(byte[] message) {
        final int idPhoneCallLength = 4;
        final int latDegreesLength = 2;
        final int latMinutesLength = 4;
        final int lonDegreesLength = 2;
        final int lonMinutesLength = 4;
        final int validityLength = 1;
        final int offerSourceLength = 1;
        final int textMessageLength = 180;

        if(message.length < HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength +
                lonDegreesLength + lonMinutesLength + validityLength + offerSourceLength +
                textMessageLength + CHECKSUM_LENGTH + PADDING_LENGTH) {
            Log.e(DEBUG_TAG, "Full message not received");
            return;
        }
        // ID Phone Call
//        byte[] tmp = new byte[idPhoneCallLength];
//        for(int i = 0; i < idPhoneCallLength; ++i) {
//            tmp[idPhoneCallLength - i - 1] = message[HEADER_LENGTH + i];
//        }
//        long idPhoneCall = ByteBuffer.wrap(tmp).getLong();
        byte[] tmp = Arrays.copyOfRange(message, HEADER_LENGTH, HEADER_LENGTH + idPhoneCallLength);
        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Latitude degrees
        tmp = new byte[4];
        tmp[0] = message[HEADER_LENGTH + idPhoneCallLength];
        tmp[1] = message[HEADER_LENGTH + idPhoneCallLength + 1];
        int latDegrees = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Latitude minutes
        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength + latDegreesLength,
                HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength);
        float latMinutes = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getFloat();

        // Longitude degrees
        tmp = new byte[4];
        tmp[0] = message[HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength];
        tmp[1] = message[HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength + 1];
        int lonDegrees = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Longitude minutes
        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength + lonDegreesLength,
                HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength + lonDegreesLength + lonMinutesLength);
        float lonMinutes = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getFloat();

        // Validity
        // Ignore

        // Source of the offer
        byte offerSource = message[HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength
                + lonDegreesLength + lonMinutesLength + validityLength];

        // Text message
        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength
                + lonDegreesLength + lonMinutesLength + validityLength + offerSourceLength,
                HEADER_LENGTH + idPhoneCallLength + latDegreesLength + latMinutesLength
                        + lonDegreesLength + lonMinutesLength + validityLength + offerSourceLength + textMessageLength);
        String textMessage = bytesToString(tmp).trim();

        float latitude = (float) latDegrees + latMinutes / 60;
        float longitude = (float) lonDegrees + lonMinutes / 60;
        broadcastLongOffer(idPhoneCall, latitude, longitude, offerSource, textMessage);
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

    private void broadcastMessage(String action, String message) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(MESSAGE, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastOffer(String action, long idPhoneCall, byte offerSource, String textMessage) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(ID_PHONE_CALL, idPhoneCall);
        intent.putExtra(OFFER_SOURCE, offerSource);
        intent.putExtra(TEXT_MESSAGE, textMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void broadcastLongOffer(long idPhoneCall, float latitude, float longitude, byte offerSource, String textMessage) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ACTION_LONG_OFFER);
        intent.putExtra(ID_PHONE_CALL, idPhoneCall);
        intent.putExtra(LATITUDE, latitude);
        intent.putExtra(LONGITUDE, longitude);
        intent.putExtra(OFFER_SOURCE, offerSource);
        intent.putExtra(TEXT_MESSAGE, textMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastVehicleStateUpdate(int state) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ACTION_VEHICLE_STATE_FOR_LOCATION_UPDATES);
        intent.putExtra(VEHICLE_STATE, state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
