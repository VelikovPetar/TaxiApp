package com.example.acer.taxiapp.services;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.acer.taxiapp.MessengerClient;
import com.example.acer.taxiapp.TCPClient;

/**
 * Created by Acer on 15.5.2017.
 */

public class TCPClientService extends Service {

    // Debug
    private String DEBUG_TAG = "TCP";
    private boolean debug = false;

    // Binder
    public final IBinder binder = new MyBinder();

    // TcpClient required components
    private TCPClient tcpClient;
    private Thread thread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void sendBytes(byte[] message) {
//        String msg = "";
//        for(byte b : loginMsg) {
//            msg += (int)b + "";
//        }
//        Log.e("MSG", msg);
        tcpClient.sendByte(message);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        tcpClient = new TCPClient(getApplicationContext());
        thread = new Thread(tcpClient);
        thread.start();
        if(debug) Log.e(DEBUG_TAG, "Service onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        if(debug) Log.e(DEBUG_TAG, "Service onDestroy");
    }

    public void stop() {
        if(tcpClient != null) {
            tcpClient.close();
            tcpClient = null;
        }
    }

    public class MyBinder extends Binder {
        public TCPClientService getService() {
            return TCPClientService.this;
        }
    }
}
