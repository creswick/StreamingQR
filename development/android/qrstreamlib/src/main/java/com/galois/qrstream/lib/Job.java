package com.galois.qrstream.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.galois.qrstream.qrpipe.StreamUtils;

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


    public void write(OutputStream out) throws IOException {
        //StreamUtils.writeShort(out, (short)0);
        byte[] mime = mimeType.getBytes();
        StreamUtils.writeShort(out, (short) mime.length);
        out.write(mime);
        StreamUtils.writeShort(out, (short) data.length);
        out.write(data);
    }

    public static Job read(InputStream in) throws IOException,
            ClassNotFoundException {
        //StreamUtils.readShort(in);
        short len = StreamUtils.readShort(in);
        String mime = StreamUtils.readString(in, len);

        len = StreamUtils.readShort(in);
        byte[] data = new byte[len];

        in.read(data);

        return new Job("", data, mime);
    }
}
