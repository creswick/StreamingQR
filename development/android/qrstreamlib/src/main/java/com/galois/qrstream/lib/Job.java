package com.galois.qrstream.lib;

import java.io.Serializable;

/**
 * Created by donp on 3/25/14.
 */
public class Job implements Serializable {
    private String title;
    private byte[] data;

    public Job(String title, byte[] data) {
        this.title = title;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public byte[] getData() {
        return data;
    }
}
