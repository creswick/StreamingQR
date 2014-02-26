package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

import com.galois.qrstream.image.BitmapImage;

import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TransmitTest {

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  @Ignore("Not ready yet") @Test
  public void testEncodeQRCodes() {
    // TODO: Ensure that we encoded sequence of QR codes correctly
    fail("testEncodeQRCodes() not implemented yet.");
  }

  @Test
  public void testEncodeQRCodesNoData() {
    Transmit transmitter = new Transmit(350,350);
    byte[] noByteData = new byte[0];

    Iterable<BitmapImage> qrCodes = transmitter.encodeQRCodes(noByteData);
    int size = 0;
    for(@SuppressWarnings("unused") BitmapImage c : qrCodes) {
       size++;
    }
    assertEquals("Empty transmission should yield no QR code", 0, size);
  }
}
