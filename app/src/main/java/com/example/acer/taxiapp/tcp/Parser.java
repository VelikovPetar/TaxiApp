package com.example.acer.taxiapp.tcp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;

import com.example.acer.taxiapp.utils.BroadcastActions;
import com.example.acer.taxiapp.activity.MainActivity;
import com.example.acer.taxiapp.R;
import com.example.acer.taxiapp.utils.Utils;
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
    public static final String TIME_IN_STATE = "time_in_state";

    // Names for broadcasts concerning status bar updates
    public static final String VALUE = "status_bar_update_value";
    public static final String COLOR = "status_bar_update_color";

    // Names for extras for offers/messages broadcasts
    public static final String ID_PHONE_CALL = "id_phone_call_extra";
    public static final String LATITUDE = "latitude_extra";
    public static final String LONGITUDE = "longitude_extra";
    public static final String SOURCE = "offer_source_extra";
    public static final String TEXT_MESSAGE = "text_message_extra";
    public static final String TIMESTAMP = "timestamp_extra";

    // Name for driver login status broadcasts
    public static final String LOGIN_STATUS = "login_status";

    private Context context;

    // Constants
    private static final int HEADER_LENGTH = 9;
    private static final int CHECKSUM_LENGTH = 2;
    private static final int PADDING_LENGTH = 5;

    public Parser(Context context) {
        this.context = context;

    }

    public void parse(byte[] message) {
        // Check if it is OK message or heartbeat message
        if (message.length > 2) {
            // Check if it is Confirmation message(BB) or incoming message(AA)
            if(message[0] == 'A' && message[1] == 'A') {
                // Process it
                // Check if the message is for this device
                if(!confirmVehicleId(message)) {
                    return;
                }

                // Get the type of the command
                String command;
                if (message.length < 9) {
                    command = "";
                } else {
                    command = (char) message[7] + "" + (char) message[8];
                }
                switch(command) {
                    case "34":
                        parseShortOffer(message);
                        break;
                    case "35":
                        parseCancelShortOffer(message);
                        break;
                    case "36":
                        parseLongOffer(message);
                        break;
                    case "40":
                        parseStatusUpdateMessage(message);
                        break;
                    case "45":
                        parsePopupMessage(message);
                        break;
                }

            } else if(message[0] == 'B' && message[1] == 'B') {
                // Confirmation of received message
            }
        }
    }

    private void parsePopupMessage(byte[] message) {

        // Calculate the length of the message text
        int lengthOfMessage = 0;
        lengthOfMessage += (message[9] - '0') * 100;
        lengthOfMessage += (message[10] - '0') * 10;
        lengthOfMessage += (message[11] - '0');

        // message[12] =!?

        // AAxxxxxyyccc(12) +
        // Validnost(1) + length(text) + source(1) // lengthOfMessage +
        // checksum(2) + zzzzz(5)
        if(message.length < HEADER_LENGTH + 3 + lengthOfMessage + CHECKSUM_LENGTH + PADDING_LENGTH) {
            return;
        }
        // SPECIAL CASE
        // Check if it is confirmation of successful login
        if(message[13] == '1' && message[14] == '1' && message[15] == '1') {
            // Confirmation of successful login
            // Display the name of the driver on the status bar
            byte[] driverNameBytes = Arrays.copyOfRange(message, 16, 12 + lengthOfMessage - 1);
            String driverName = bytesToString(driverNameBytes);
            broadcastStatusUpdate(BroadcastActions.ACTION_DRIVER_STATUS, new StatusBarFragment.DriverStatusValue(driverName, Utils.getColor(context, R.color.green)));
            broadcastLoginAction(true);
        // SPECIAL CASE
        // Check if it is confirmation of successful logout
        } else if(message[13] == '0' && message[14] == '0' && message[15] == '0') {
            broadcastStatusUpdate(BroadcastActions.ACTION_DRIVER_STATUS, new StatusBarFragment.DriverStatusValue(context.getString(R.string.no_logged_driver), Utils.getColor(context, R.color.yellow)));
            broadcastLoginAction(false);
        } else {
            // Regular popup message
            byte[] popupMessageTextBytes = Arrays.copyOfRange(message, 13, 12 + lengthOfMessage - 1);
            String popupMessageText = bytesToString(popupMessageTextBytes);

            // SPECIAL CASE
            // If the message contains "(Ne igraj so kartickata !)", that means that the driver is already
            // logged in. If the driver has remained logged in the previous usage of the application,
            // display his name in the status bar
            if(popupMessageText.contains(context.getString(R.string.dont_play_with_the_card))) {
                broadcastStatusUpdate(BroadcastActions.ACTION_DRIVER_STATUS,
                        new StatusBarFragment.DriverStatusValue(popupMessageText.replace(context.getString(R.string.dont_play_with_the_card), "").trim(), Utils.getColor(context, R.color.green)));
                broadcastLoginAction(true);
            }
            // SPECIAL CASE
            // The server returned "Invalid card" message
            if(popupMessageText.contains(context.getString(R.string.invalid_card))) {
                broadcastMessage(BroadcastActions.ACTION_INVALID_CARD, (byte) -1, null, null);
                return;
            }

            // Calculate the time
            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String timeStamp = String.format(Locale.getDefault(), "%02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            // Check for the message source
            byte source = message[12 + lengthOfMessage - 1];
            // Broadcast message
            broadcastMessage(BroadcastActions.ACTION_POPUP_MESSAGE, source, popupMessageText, timeStamp);
        }
    }

    private void parseStatusUpdateMessage(byte[] message) {
        final int stateLength = 20;

        // AAxxxxxyy(9)
        int startPos = 9;
        if(message.length < HEADER_LENGTH + stateLength + 2 + 1 + 1 + CHECKSUM_LENGTH + PADDING_LENGTH) {
            return;
        }
        String newState = "";
        for(int i = startPos; i < startPos + stateLength; ++i) {
            if(message[i] == 0)
                break;
            newState += (char) message[i];
        }

        // Vreme vo sostojba
        byte[] tmp = new byte[4];
        tmp[0] = message[startPos + stateLength];
        tmp[1] = message[startPos + stateLength + 1];
        int timeInState = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Prodolzhuvanje na vreme
        // message[startPos + stateLength + 2] ?!

        // Sostojba
        // For status updates that activate countdown timers
        broadcastVehicleStateUpdate((int) message[startPos + stateLength + 3], timeInState);

        // Show status on the Status bar
        broadcastStatusUpdate(BroadcastActions.ACTION_VEHICLE_STATE_STATUS, new StatusBarFragment.VehicleStatusValue(newState.replace(context.getString(R.string.state), ""), Color.CYAN));
    }

    private void parseShortOffer(byte[] message) {
        final int idPhoneCallLength = 4;
        final int offerSourceLength = 1;
        final int textMessageLength = 60;
        if(message.length < HEADER_LENGTH + idPhoneCallLength + offerSourceLength + textMessageLength + CHECKSUM_LENGTH + PADDING_LENGTH) {
            return;
        }

        byte[] tmp = new byte[8];
        tmp[0] = message[HEADER_LENGTH];
        tmp[1] = message[HEADER_LENGTH + 1];
        tmp[2] = message[HEADER_LENGTH + 2];
        tmp[3] = message[HEADER_LENGTH + 3];
        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getLong();

        byte offerSource = message[HEADER_LENGTH + idPhoneCallLength];
        tmp = Arrays.copyOfRange(message, HEADER_LENGTH + idPhoneCallLength + offerSourceLength, HEADER_LENGTH + idPhoneCallLength + offerSourceLength + textMessageLength);
        String textMessage = bytesToString(tmp).trim();
        broadcastOffer(BroadcastActions.ACTION_SHORT_OFFER, idPhoneCall, offerSource, textMessage);
    }

    private void parseCancelShortOffer(byte[] message) {
        final int idPhoneCallLength = 4;
        final int textMessageLength = 60;
        if(message.length < HEADER_LENGTH + idPhoneCallLength + textMessageLength + CHECKSUM_LENGTH + PADDING_LENGTH) {
            return;
        }

        byte[] tmp = new byte[8];
        tmp[0] = message[HEADER_LENGTH];
        tmp[1] = message[HEADER_LENGTH + 1];
        tmp[2] = message[HEADER_LENGTH + 2];
        tmp[3] = message[HEADER_LENGTH + 3];
        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getLong();

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
            return;
        }

        byte[] tmp = new byte[8];
        tmp[0] = message[HEADER_LENGTH];
        tmp[1] = message[HEADER_LENGTH + 1];
        tmp[2] = message[HEADER_LENGTH + 2];
        tmp[3] = message[HEADER_LENGTH + 3];
        long idPhoneCall = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getLong();

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
            return false;
        }
        byte[] deviceIdBytes = Arrays.copyOfRange(message, 2, 7);
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String deviceId = preferences.getString(MainActivity.DEVICE_ID, null);
        if(deviceId == null)
            return false;
        return deviceId.equals(bytesToString(deviceIdBytes));
    }

    private void broadcastLoginAction(boolean loggedIn) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ACTION_DRIVER_LOGIN_STATUS);
        intent.putExtra(LOGIN_STATUS, loggedIn);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastStatusUpdate(String action, StatusBarFragment.StatusUpdate statusUpdate) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(VALUE, statusUpdate.getValue());
        intent.putExtra(COLOR, statusUpdate.getColor());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastMessage(String action, byte messageSource, String textMessage, String timestamp) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(SOURCE, messageSource);
        intent.putExtra(MESSAGE, textMessage);
        intent.putExtra(TIMESTAMP, timestamp);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastOffer(String action, long idPhoneCall, byte offerSource, String textMessage) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(ID_PHONE_CALL, idPhoneCall);
        intent.putExtra(SOURCE, offerSource);
        intent.putExtra(TEXT_MESSAGE, textMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastLongOffer(long idPhoneCall, float latitude, float longitude, byte offerSource, String textMessage) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ACTION_LONG_OFFER);
        intent.putExtra(ID_PHONE_CALL, idPhoneCall);
        intent.putExtra(LATITUDE, latitude);
        intent.putExtra(LONGITUDE, longitude);
        intent.putExtra(SOURCE, offerSource);
        intent.putExtra(TEXT_MESSAGE, textMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastVehicleStateUpdate(int state, int timeInState) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ACTION_VEHICLE_STATE_FOR_TIMERS);
        intent.putExtra(VEHICLE_STATE, state);
        intent.putExtra(TIME_IN_STATE, timeInState);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
