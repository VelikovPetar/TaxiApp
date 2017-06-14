package com.example.acer.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.fragments.StatusBarFragment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


public class TCPClient implements Runnable {

    // Debug
    private String DEBUG_TAG = "TCP";
    private boolean debug = true;

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

    // Attempts to reconnect to server
    private int serverReconnectAttempts = 0;
    private volatile boolean serverAvailable = false;

    // Reader and writer from the socket
//    private InputStream reader;
//    private OutputStream writer;
    private InputStream reader;
    private DataOutputStream writer;

    // Context of the foreground activity
    private Context context;

    // Socket
    private Socket socket;

    // Instance
    private static TCPClient instance;

    // Singleton getter
    public static TCPClient getInstance(Context context) {
        if(instance == null) {
            instance = new TCPClient(context.getApplicationContext());
        }
        return instance;
    }

    // Constructor
    private TCPClient(Context context) {
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

    public boolean isServerAvailable() {
        return serverAvailable;
    }

    public void sendBytes(byte[] message) {
        synchronized (this) {
            try {
                if(!Utils.hasInternetConnection(context)) {
                    Log.e("MSG", "No connection");
                    return;
                }
                if(message == null) {
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

        shouldAutomaticallyReconnect = true;

        if(debug)Log.e(DEBUG_TAG, "sar = " + shouldAutomaticallyReconnect);

        while(shouldAutomaticallyReconnect) {

            if(debug)Log.e(DEBUG_TAG, "Client started");

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
                // Edge case
                // If the internet connection is lost, and the app is closed, but still in memory,
                // The thread may remain alive, and if connection is obtained, a tcp connection to the server may be
                // established. Because closing the app sets shouldAutomaticallyReconnect to false,
                // this will make sure the thread finishes working.
                if(!shouldAutomaticallyReconnect) {
                    return;
                }
            }

            // Notify user that internet connection WAS established
            isWaitingData = true;
            broadcastStatusUpdate(BroadcastActions.ACTION_CONNECTION_STATUS, StatusBarFragment.ConnectionStatusValues.CONNECTED);
            if(debug)Log.e(DEBUG_TAG, "Has connection");

            // Connecting to server
            serverAvailable = false;
            broadcastStatusUpdate(BroadcastActions.ACTION_SERVER_STATUS, StatusBarFragment.ServerStatusValues.CONNECTING);
            Log.e("SERVERATT", ""+serverReconnectAttempts);
            if(serverReconnectAttempts == 3) {
                // If there were 3 failed reconnects, wait 1 minute before trying again
                serverReconnectAttempts = 0;
                broadcastStatusUpdate(BroadcastActions.ACTION_SERVER_STATUS, StatusBarFragment.ServerStatusValues.NOT_CONNECTED);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(serverReconnectAttempts > 0 && serverReconnectAttempts < 3){
                serverAvailable = false;
                // Try to connect to server after waiting 3 seconds
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Edge case
            // If the connection to the server is lost, and the app is closed, but still in memory,
            // The thread may remain alive, and if connection is obtained, a tcp connection to the server may be
            // established. Because closing the app sets shouldAutomaticallyReconnect to false,
            // this will make sure the thread finishes working.
            if(!shouldAutomaticallyReconnect) {
                return;
            }

            try {
                // Open the socket
                socket = new Socket(SERVER_IP, SERVER_PORT);
                socket.setSoTimeout(TIMEOUT);

                // If the socket is opened display that the server is OK
                serverReconnectAttempts = 0;
                serverAvailable = true;
                broadcastStatusUpdate(BroadcastActions.ACTION_SERVER_STATUS, StatusBarFragment.ServerStatusValues.CONNECTED);

                if(debug)Log.e(DEBUG_TAG, "Socket established");

                // Open reader
                reader = socket.getInputStream();

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
//                    while((count = socket.getInputStream().read(buffer, 0, buffer.length)) > 0) {
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

                    byte[] buffer = new byte[512];
                    int readBytes = reader.read(buffer);
                    ArrayList<Byte> bytes = new ArrayList<>();
                    for(int i = 0; i < readBytes; ++i) {
                        // Case when multiple messages come appended to each other
                        if(i < readBytes - 1) {
                            if((buffer[i] == 'A' && buffer[i + 1] == 'A') || (buffer[i] == 'B' && buffer[i + 1] == 'B')) {
                                if(bytes.size() > 0) {
                                    parser.parse(listToArray(bytes));
                                    bytes = new ArrayList<>();

                                    Log.e(DEBUG_TAG, message);
                                    message = "";
                                }
                            }
                        }
                        bytes.add(buffer[i]);
                        message += (char) buffer[i];
                    }
                    // Parse the received message
                    parser.parse(listToArray(bytes));
                    if(debug)Log.e(DEBUG_TAG, "RECEIVED: " + message);

                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                // Notify user that there is probably server-side problem
                if(debug) Log.e(DEBUG_TAG, "SocketException");
            } catch (IOException e) {
                e.printStackTrace();
                // Identify the problem
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                if(Utils.hasInternetConnection(context)) {
                    // TODO Notify of probable server error
                    Log.e("SERVERATT", "HERE");
                    serverReconnectAttempts ++;
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
                if(!shouldAutomaticallyReconnect) {
                    serverReconnectAttempts = 0;
                }
            }
        }
    }

    private byte[] listToArray(ArrayList<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); ++i) {
            ret[i] = bytes.get(i);
        }
        return ret;
    }
}
