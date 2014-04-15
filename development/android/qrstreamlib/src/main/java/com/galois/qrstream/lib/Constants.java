package com.galois.qrstream.lib;

/**
 * Created by donp on 3/3/14.
 */
public final class Constants {
    public static final String APP_TAG = "qrstream";
    public static final int RECEIVE_TIMEOUT_MS = 10000;

    // Updates frame of QR code at regular interval
    public static final int TRANSMIT_INTERVAL_MS = 800;

    // Do not allow class to be instantiated.
    // Reference constants by Constants.APP_TAG
    private Constants() {
        throw new AssertionError("Unexpected instantiation of private Constants class.");
    }
}
