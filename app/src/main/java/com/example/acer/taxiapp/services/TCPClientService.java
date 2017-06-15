package com.example.acer.taxiapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.example.acer.taxiapp.TCPClient;

public class TCPClientService extends Service {

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
        tcpClient.sendBytes(message);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tcpClient = TCPClient.getInstance(this);
        thread = new Thread(tcpClient);
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
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
