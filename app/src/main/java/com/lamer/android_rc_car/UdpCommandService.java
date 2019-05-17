package com.lamer.android_rc_car;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UdpCommandService extends CommandService {
    // Debugging
    private static final String TAG = "UDPCommandService";
    private static final boolean D = true;
    private final Handler mHandler;
    private final Context mContext;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public UdpCommandService(Context context, Handler handler) {
        mState = STATE_NONE;
        mContext = context;
        //mConnectionLostCount = 0;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     */
    public synchronized void connect() {
        if (D) Log.d(TAG, "connect to UDP");

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            mConnectThread.send();
            return;
            //if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        if (mConnectThread != null) {
            return;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread();
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param ip  The ip on which the connection was made
     * @param port  The port that has been connected
     */
    public synchronized void connected(String ip, int port) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(ip,port);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, ip+":"+port);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The String to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(String out) {
        if(out==null)return;
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out.getBytes());
        Log.v(TAG, "write message ["+ out +"] to car");
    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void write(int out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
//        mConnectionLostCount++;
//        if (mConnectionLostCount < 3) {
//          // Send a reconnect message back to the Activity
//          Message msg = mHandler.obtainMessage(RemoteBluetooth.MESSAGE_TOAST);
//          Bundle bundle = new Bundle();
//          bundle.putString(RemoteBluetooth.TOAST, "Device connection was lost. Reconnecting...");
//          msg.setData(bundle);
//          mHandler.sendMessage(msg);
//
//          connect(mSavedDevice);
//        } else {
        if(mState == STATE_NONE)return;
        setState(STATE_LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
//        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final String ipAddress;
        private BroadcastListener listener;
        private DatagramSocket socket;
        private static final int PORT = 8888;
        private static final int INPORT = 8889;

        public ConnectThread(){
            this.listener = new BroadcastListener() {
                @Override
                public void onReceive(String msg, String ip) {
                    Log.v(TAG, "receive message "+msg+" from "+ip);
                    if(msg!=null && msg.contains("ACK")){
                        connected(ip,PORT);
                    }
                }
            };
            WifiManager wifiMgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            ipAddress = Formatter.formatIpAddress(ip);
            Log.v(TAG, "UDP ready from "+ipAddress);
        }

        public void send(){
            try {
                send("#1#95#2#100");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void send(String msg) throws IOException {
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setBroadcast(true);
            byte[] sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, getBroadcastAddress(), PORT);//getBroadcastAddress()
            //InetAddress.getByName("192.168.1.255")
            clientSocket.send(sendPacket);
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(INPORT);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            try {
                send("#1#95#2#100");
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!socket.isClosed()) {
                try {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    if(!packet.getAddress().getHostAddress().equals(ipAddress)) {
                        listener.onReceive(
                                new String(packet.getData(), 0, packet.getLength()),
                                packet.getAddress().getHostAddress());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            socket.close();
        }
        public void cancel() {
            socket.close();
        }

        InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();
            if(dhcp == null)
                return InetAddress.getByName("255.255.255.255");
            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            return InetAddress.getByAddress(quads);
        }
    }

    public interface BroadcastListener {
        public void onReceive(String msg, String ip);
    }
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final String ipAddress;
        private final int port;
        private DatagramSocket socket;
        private BroadcastListener listener;
        private static final int INPORT = 8889;

        public ConnectedThread(String ip,int port) {
            Log.d(TAG, "create ConnectedThread");
            ipAddress = ip;
            this.port = port;
            this.listener = new BroadcastListener() {
                @Override
                public void onReceive(String msg, String ip) {
                    Log.v(TAG, "receive message "+msg+" from "+ip);
                    if(msg!=null && msg.contains("ACK")){
                        mHandler.obtainMessage(MESSAGE_READ, msg.length(), -1, msg)
                                .sendToTarget();
                    }else{
                        connectionLost();
                    }
                }
            };
        }

        public void run() {
            try {
                socket = new DatagramSocket(INPORT);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            while (!socket.isClosed()) {
                try {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    if(packet.getAddress().getHostAddress().equals(ipAddress)) {
                        listener.onReceive(
                                new String(packet.getData(), 0, packet.getLength()),
                                packet.getAddress().getHostAddress());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                }
            }
            socket.close();
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            if(buffer==null || buffer.length==0)return;
            DatagramSocket clientSocket = null;
            try {
                clientSocket = new DatagramSocket();
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ipAddress), port);
                clientSocket.send(sendPacket);
            } catch (SocketException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(int out) {
            write(Integer.valueOf(out).byteValue());
        }

        public void cancel() {
            socket.close();
        }

    }
}
