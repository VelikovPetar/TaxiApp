package com.example.acer.taxiapp.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.Utils;
import com.example.acer.taxiapp.fragments.StatusBarFragment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClientIntentService extends IntentService {


    // Debug
    private String DEBUG_TAG = "TCP";
    private boolean debug = true;

    // Name for the messages that are broadcast by the TcpClient
    public static final String MESSAGE = "broadcast_message";

    // Name for broadcasts concerning status bar updates
    public static final String VALUE = "status_bar_update_value";
    public static final String COLOR = "status_bar_update_color";


    // Server information
    private static final String SERVER_IP = "62.162.54.11";
    private static final int SERVER_PORT = 2004;
    private static final int TIMEOUT = 50000;

    // Indicates whether the socket is waiting incoming data
    private volatile boolean isWaitingData;

    // Reader and writer from the socket
//    private InputStream reader;
//    private OutputStream writer;
    private BufferedReader reader;
    private DataOutputStream writer;

    // Socket
    private Socket socket;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public TCPClientIntentService(String name) {
        super(name);
    }

    public TCPClientIntentService() {
        super("Intent service");
    }

    // Manually close the connection
    public void close() {

        if(debug) Log.e(DEBUG_TAG, "In close");

        if(isWaitingData) {
            isWaitingData = false;

            if(debug)Log.e(DEBUG_TAG, "Falsed booleans");

            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(debug)Log.e(DEBUG_TAG, "Closed socket");

            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(debug)Log.e(DEBUG_TAG, "Closed streams");

            reader = null;
            writer = null;
            socket = null;

            if(debug)Log.e(DEBUG_TAG, "Nulled everything");
        }
    }

    public void sendByte(byte[] message) {
        synchronized (this) {
            try {
                writer.write(message);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastMessage(String action, String value) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(MESSAGE, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastStatusUpdate(String action, StatusBarFragment.StatusUpdate statusUpdate) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(VALUE, statusUpdate.getValue());
        intent.putExtra(COLOR, statusUpdate.getColor());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class TCPServiceBinder extends Binder {

        public TCPClientIntentService getService() {
            return TCPClientIntentService.this;
        }
    }

    private TCPServiceBinder binder = new TCPServiceBinder();

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(debug)Log.e(DEBUG_TAG, "Client started");

        if(Utils.hasInternetConnection(this)) {
            // TODO
            // Notify user that internet connection WAS established
            isWaitingData = true;
            if (debug) Log.e(DEBUG_TAG, "Has connection");
        }
        try {
            // Open the socket
            socket = new Socket(SERVER_IP, SERVER_PORT);
            socket.setSoTimeout(TIMEOUT);

            if(debug)Log.e(DEBUG_TAG, "Socket established");

            // Open reader
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Open writer
            writer = new DataOutputStream(socket.getOutputStream());

            if(debug)Log.e(DEBUG_TAG, "Acquired I/O streams");

            // Waiting for incoming data
            while(isWaitingData) {

                if(debug)Log.e(DEBUG_TAG, "Is waiting data...");

                String message = "";
//                    byte[] buffer = new byte[256];
//                    byte[] receivedMessage = new byte[0];
//                    int count = 0;
//                    while((count = reader.read(buffer)) > 0) {
//                        byte[] tmp = new byte[receivedMessage.length + count];
//                        for(int i = 0; i < receivedMessage.length; ++i) {
//                            tmp[i] = receivedMessage[i];
//                        }
//                        for(int i = 0; i < count; ++i) {
//                            tmp[receivedMessage.length + i] = buffer[i];
//                        }
//                        receivedMessage = tmp;
//                    }
//                    for(byte b : receivedMessage) {
//                        message += (char) b;
//                    }

                char[] buffer = new char[256];
                int readChars = reader.read(buffer);
                for(int i = 0; i < readChars; ++i) {
                    message += buffer[i];
                }

                if(debug)Log.e(DEBUG_TAG, "RECEIVED: " + message);
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            // TODO
            // Notify user that there is probably server-side problem
            if(debug) Log.e(DEBUG_TAG, "SocketException");
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
            // Identify the problem
            if(Utils.hasInternetConnection(this)) {
                // TODO Notify of probable server error
            } else {
                // TODO Notify that connection was lost on the local device
            }
            if(debug) Log.e(DEBUG_TAG, "IOException");
            if(debug) Log.e(DEBUG_TAG, e.getLocalizedMessage());
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
