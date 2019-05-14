package com.lamer.android_rc_car;

public abstract class CommandService {
    // Constants that indicate command to computer
    public static final int EXIT_CMD = -1;
    public static final int VOL_UP = 1;
    public static final int VOL_DOWN = 2;
    public static final int MOUSE_MOVE = 3;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "HC-06";
    public static final String TOAST = "toast";
    protected int mState;

    public abstract void stop();

    public int getState() {
        return mState;
    }

    public abstract void start();

    public abstract void write(String s);
}
