package com.example.acer.taxiapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MessengerClient {

    private static byte[] getBaseCommonMessage(Location location, Context context) {
        byte[] message = new byte[71];

        // Najava na paket
        message[0] = message[1] = (byte) 'A';

        // Broj na ured
        // TODO Da se prochita ID=na uredot samo ednashka i da se chuva vo memorija
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String deviceID = preferences.getString(MainActivity.DEVICE_ID, null);
        byte[] bytes;
        if(deviceID != null) {
            bytes = deviceID.getBytes();
        } else {
            bytes = new byte[5];
        }
        message[2] = bytes[0];
        message[3] = bytes[1];
        message[4] = bytes[2];
        message[5] = bytes[3];
        message[6] = bytes[4];

        // Komanda
        message[7] = (byte) '0';
        message[8] = (byte) '8';

        // Podatoci
        // Stari podatoci
        int i = 1;
        int bits = Float.floatToIntBits(i);
        message[9] = (byte) (i);
        message[10] = (byte) (i >> 8);
        message[11] = (byte) (i >> 16);
        message[12] = (byte) (i >> 24);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(location.getTime()));
//        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        // UTC den
        message[13] = (byte) cal.get(Calendar.DAY_OF_MONTH);
        message[14] = (byte) (cal.get(Calendar.MONTH) + 1);
        message[15] = (byte) (cal.get(Calendar.YEAR) - 2000);
        message[16] = (byte) cal.get(Calendar.HOUR_OF_DAY);
        message[17] = (byte) cal.get(Calendar.MINUTE);
        message[18] = (byte) cal.get(Calendar.SECOND);

        // GPS data
        double latitude = location.getLatitude();
        int degrees = (int) latitude;
        message[19] = (byte) (degrees);
        message[20] = (byte) (degrees >> 8);

        float minutes = (float) (latitude - degrees) * 60;
        bits = Float.floatToIntBits(minutes);
        message[21] = (byte) (bits);
        message[22] = (byte) (bits >> 8);
        message[23] = (byte) (bits >> 16);
        message[24] = (byte) (bits >> 24);

        // latitude direction
        message[25] = 'N';

        double longitude = location.getLongitude();
        degrees = (int) longitude;
        message[26] = (byte) (degrees);
        message[27] = (byte) (degrees >> 8);

        minutes = (float) (longitude - degrees) * 60;
        bits = Float.floatToIntBits(minutes);
        message[28] = (byte) (bits);
        message[29] = (byte) (bits >> 8);
        message[30] = (byte) (bits >> 16);
        message[31] = (byte) (bits >> 24);

        // longitude direction
        message[32] = 'E';

        // Speed
        float speed = location.getSpeed();
        bits = Float.floatToIntBits(speed);
        message[33] = (byte) (bits);
        message[34] = (byte) (bits >> 8);
        message[35] = (byte) (bits >> 16);
        message[36] = (byte) (bits >> 24);

        // Number of satellites
        int satellites = location.getExtras().getInt("satellites");
        message[37] = (byte) (satellites);
        message[38] = (byte) (satellites >> 8);

        // HDOP
        float hdop = location.getAccuracy();
        bits = Float.floatToIntBits(hdop);
        message[39] = (byte) (bits);
        message[40] = (byte) (bits >> 8);
        message[41] = (byte) (bits >> 16);
        message[42] = (byte) (bits >> 24);

        // Visina TODO
        int altitude = (int) location.getAltitude();
        message[43] = (byte) (altitude);
        message[44] = (byte) (altitude >> 8);

        // Bearing TODO
        float bearing = location.getBearing();
        bits = Float.floatToIntBits(bearing);
        message[45] = (byte) (bits);
        message[46] = (byte) (bits >> 8);
        message[47] = (byte) (bits >> 16);
        message[48] = (byte) (bits >> 24);

        // Binary data 1
        message[49] = setBinaryData1ForCommonMessage();

        // Binary data 2
        message[50] = setBinaryData2ForCommonMessage();

        // Analog data
        int analogData = 0;
        message[51] = (byte) (analogData);
        message[52] = (byte) (analogData >> 8);


        // KM GPS
        int kmGps = 0;
        message[53] = (byte) (kmGps);
        message[54] = (byte) (kmGps >> 8);
        message[55] = (byte) (kmGps >> 16);
        message[56] = (byte) (kmGps >> 24);

        // KM TAXI
        int kmTaxi = 0;
        message[57] = (byte) (kmTaxi);
        message[58] = (byte) (kmTaxi >> 8);
        message[59] = (byte) (kmTaxi >> 16);
        message[60] = (byte) (kmTaxi >> 24);

        // ID Card
        message[61] = '0';
        message[62] = '0';
        message[63] = '0';
        message[64] = '0';
        message[65] = '0';
        message[66] = '0';
        message[67] = '0';
        message[68] = '0';
        message[69] = '0';
        message[70] = '0';

        return message;
    }

    public static byte[] getLoginMessage(Location location, Context context) {
        byte[] message = getBaseCommonMessage(location, context);
        message[49] = setBinaryData1ForLoginMessage();
        message[50] = setBinaryData2ForLoginMessage();

        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String driverID = preferences.getString(MainActivity.RF_CARD_ID, null);
        byte[] bytes;
        if(driverID != null) {
            bytes = driverID.getBytes();
        } else {
            bytes = new byte[4];
        }
        message[67] = bytes[0];
        message[68] = bytes[1];
        message[69] = bytes[2];
        message[70] = bytes[3];

        byte[] res = addChkSum(message);
        return res;
    }

    // TODO CHECK?!
    public static byte[] getLogoutMessage(Location location, Context context) {
        byte[] message = getBaseCommonMessage(location, context);
        message[49] = setBinaryData1ForLogoutMessage();
        message[50] = setBinaryData2ForLogoutMessage();

        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String driverID = preferences.getString(MainActivity.RF_CARD_ID, null);
        byte[] bytes;
        if(driverID != null) {
            bytes = driverID.getBytes();
        } else {
            bytes = new byte[4];
        }
        message[67] = bytes[0];
        message[68] = bytes[1];
        message[69] = bytes[2];
        message[70] = bytes[3];

        byte[] res = addChkSum(message);
        return res;
    }

    // TODO FIX?!
    public static byte[] getPauseStartMessage(Location location, Context context) {
        byte[] message = getBaseCommonMessage(location, context);

        message[49] = setBinaryData1ForLogoutMessage();
        message[50] = setBinaryData2ForLogoutMessage();

        byte[] res = addChkSum(message);
        return res;
    }

    // TODO FIX?!
    public static byte[] getPauseStopMessage(Location location, Context context) {
        byte[] message = getBaseCommonMessage(location, context);

        message[49] = setBinaryData1ForLoginMessage();
        message[50] = setBinaryData2ForLogoutMessage();

        byte[] res = addChkSum(message);
        return res;
    }



    private static byte[] addChkSum(byte[] message) {
        byte[] retVal = new byte[message.length + 2];
//        byte[] retVal = new byte[message.length + 7];

        for (int i = 0; i < message.length; ++i)
        {
            retVal[i] = message[i];
        }

        byte tmpByte = (byte)0;


        for (byte item : message)
        {
            tmpByte = (byte)(tmpByte ^ item);
        }

        retVal[retVal.length - 1] = (byte)((tmpByte & 0x0f) | 0x30);
        retVal[retVal.length - 2] = (byte)(((tmpByte & 0xf0) >> 4) | 0x30);

//        retVal[retVal.length - 6] = (byte)((tmpByte & 0x0f) | 0x30);
//        retVal[retVal.length - 7] = (byte)(((tmpByte & 0xf0) >> 4) | 0x30);

//        for (int i = 5; i > 0; i--)
//        {
//            retVal[retVal.length - i] = (byte)'z';
//        }

        return retVal;
    }

    private static byte setBinaryData1ForCommonMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        res |= 1; // FIKSNO
        // Za odlogiranje
        // res |= 1 << 5;
        return res;
    }

    private static byte setBinaryData2ForCommonMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        // Za iskluchen taksimetar
        // res |= 1
        // TODO
        return res;
    }

    private static byte setBinaryData1ForLoginMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        res |= 1; // FIKSNO
        return res;
    }

    private static byte setBinaryData2ForLoginMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        return res;
    }

    private static byte setBinaryData1ForLogoutMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        res |= 1; // FIKSNO
        // Za odlogiranje
        res |= 1 << 5;
        return res;
    }

    private static byte setBinaryData2ForLogoutMessage() {
        byte res = 0;
        res |= 1 << 7; // FIKSNO
        res |= 1 << 6; // FIKSNO
        // Za iskluchen taksimetar
        res |= 1 << 1;
        return res;
    }

}
