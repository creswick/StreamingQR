package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

//QR code reader/writer
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ReceiveTest {

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  @Test
  public void dummyTestSoBuildPasses() {
	  // TODO: remove once a test is ready.
    assertTrue(false);
  }
  
  @Ignore("Not ready yet") @Test
  public void testDecodeQRCodes() {
    // TODO: Ensure that we decoded sequence of QR codes correctly
    fail("testDncodeQRCodes() not implemented yet.");
  }
}
