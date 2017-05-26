package com.example.acer.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.fragments.StatusBarFragment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class TCPClient implements Runnable {

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

    // Indicates whether the service should run uninterrupted and automatically attempt
    // to reconnect if it disconnects
    private volatile boolean shouldAutomaticallyReconnect;

    // Reader and writer from the socket
//    private InputStream reader;
//    private OutputStream writer;
    private BufferedReader reader;
    private DataOutputStream writer;

    // Context of the foreground activity
    private Context context;

    // Socket
    private Socket socket;

    // Constructor
    public TCPClient(Context context) {
        this.context = context;
        this.isWaitingData = false;
        this.shouldAutomaticallyReconnect = true;
    }

    // Manually close the connection
    public void close() {

        if(debug)Log.e(DEBUG_TAG, "In close");

        if(isWaitingData) {
            isWaitingData = false;
            shouldAutomaticallyReconnect = false;

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
                if(!Utils.hasInternetConnection(context)) {
                    Log.e("MSG", "No connection");
                    return;
                }
                if(writer != null) {
                    writer.write(message);
                    writer.flush();
                }
                String msg = "";
                for(byte b : message) {
                    msg += (int)b + "";
                }
                Log.e("MSG", msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastStatusUpdate(String action, StatusBarFragment.StatusUpdate statusUpdate) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(VALUE, statusUpdate.getValue());
        intent.putExtra(COLOR, statusUpdate.getColor());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void run() {

        if(debug)Log.e(DEBUG_TAG, "sar = " + shouldAutomaticallyReconnect);

        while(shouldAutomaticallyReconnect) {

            if(debug)Log.e(DEBUG_TAG, "Client started");

            // TODO Treba da se osmisli podobro ako e vozmozhno!!!
            // Wait for internet connection
            int attempts = 0;
            while(!Utils.hasInternetConnection(context)) {
                broadcastStatusUpdate(BroadcastActions.ACTION_CONNECTION_STATUS, StatusBarFragment.ConnectionStatusValues.CONNECTING);
                // Check every 1 second for internet connection for 10 seconds,
                // then wait for 5 seconds before trying again
                try {
                    Thread.sleep(1000);
                    if(attempts++ == 10) {
                        broadcastStatusUpdate(BroadcastActions.ACTION_CONNECTION_STATUS, StatusBarFragment.ConnectionStatusValues.NOT_CONNECTED);
                        Thread.sleep(5000);
                        attempts = 0;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Notify user that internet connection WAS established
            isWaitingData = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_CONNECTION_STATUS, StatusBarFragment.ConnectionStatusValues.CONNECTED);

            if(debug)Log.e(DEBUG_TAG, "Has connection");

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

                // Create parser
                Parser parser = new Parser(context);

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
                        if(i < readChars - 1) {
                            if(buffer[i] == 'A' && buffer[i + 1] == 'A') {
                                if(message.length() > 2) {
                                    if(debug)Log.e(DEBUG_TAG, "RECEIVED: " + message);
                                    parser.parse(message.getBytes());
                                    message = "";
                                }
                            }
                        }
                        message += buffer[i];
                    }

                    // Parse the received message
                    parser.parse(message.getBytes());
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
                if(Utils.hasInternetConnection(context)) {
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
}
