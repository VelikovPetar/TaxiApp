package com.example.acer.taxiapp;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.acer.taxiapp.fragments.StatusBarFragment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


public class TCPClient implements Runnable {

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

        if(isWaitingData) {
            isWaitingData = false;
            shouldAutomaticallyReconnect = false;

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

            reader = null;
            writer = null;
            socket = null;
        }
    }

    public boolean sendBytes(byte[] message) {
        synchronized (this) {
            try {
                if(!Utils.hasInternetConnection(context)) {
                    return false;
                }
                if(message == null) {
                    return false;
                }
                if(writer != null) {
                    writer.write(message);
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
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
        while(shouldAutomaticallyReconnect) {
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

            // Connecting to server
            broadcastStatusUpdate(BroadcastActions.ACTION_SERVER_STATUS, StatusBarFragment.ServerStatusValues.CONNECTING);
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

                // If the socket is opened, display that the server is OK
                serverReconnectAttempts = 0;
                broadcastStatusUpdate(BroadcastActions.ACTION_SERVER_STATUS, StatusBarFragment.ServerStatusValues.CONNECTED);

                // Open reader
                reader = socket.getInputStream();
                // Open writer
                writer = new DataOutputStream(socket.getOutputStream());

                // Create parser
                Parser parser = new Parser(context);

                // Waiting for incoming data
                while(isWaitingData) {
                    byte[] buffer = new byte[1024];
                    int readBytes = reader.read(buffer);
                    ArrayList<Byte> bytes = new ArrayList<>();
                    String msg = "";
                    for(int i = 0; i < readBytes; ++i) {
                        // Case when multiple messages come appended to each other
                        if(i < readBytes - 1) {
                            if((buffer[i] == 'A' && buffer[i + 1] == 'A') || (buffer[i] == 'B' && buffer[i + 1] == 'B')) {
                                if(bytes.size() > 0) {
                                    parser.parse(listToArray(bytes));
                                    bytes = new ArrayList<>();
                                    Log.e("Incoming msg", msg);
                                    msg = "";
                                }
                            }
                        }
                        bytes.add(buffer[i]);
                        msg += (char) buffer[i];
                    }
                    // Parse the received message
                    parser.parse(listToArray(bytes));
                    Log.e("Incoming msg", msg);
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                // Notify user that there is probably server-side problem
                serverReconnectAttempts++;
            } catch (IOException e) {
                e.printStackTrace();
                // Identify the problem
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                if(Utils.hasInternetConnection(context)) {
                    // If the device has internet connection,
                    // there is a probable problem with the server
                    serverReconnectAttempts ++;
                }
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
