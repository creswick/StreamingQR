package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

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

}
