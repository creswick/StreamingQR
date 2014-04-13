package com.galois.qrstream.lib;

import java.io.Serializable;

/**
 * Created by donp on 3/25/14.
 */
public class Job implements Serializable {
    private String title;
    private byte[] data;
    private String mimeType;

    public Job(String title, byte[] data, String mimeType) {
        this.title = title;
        this.data = data;
        this.mimeType = mimeType;
    }

    public String getMimeType() { return mimeType; }

    public String getTitle() {
        return title;
    }

    public byte[] getData() {
        return data;
    }

    public String toString() {
        return title+": "+mimeType;
    }
}
