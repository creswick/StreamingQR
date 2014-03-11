package com.galois.qrstream.qrpipe;


import static org.junit.Assert.*;

//QR code reader/writer
import com.google.zxing.BarcodeFormat;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ReceiveTest {

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  @Test
  public void dummyTestSoBuildPasses() {
	  // TODO: remove once a test is ready.
  }

  @Ignore("Not ready yet") @Test
  public void testDecodeQRCodes() {
    // TODO: Ensure that we decoded sequence of QR codes correctly
    fail("testDncodeQRCodes() not implemented yet.");
  }

  /**
   * Try decoding standard QR code without any of the
   * sequence information inserted into payload.
   */
  @Test
  public void testDecodeQrWithNoSequenceInfo() {
    // Decode qr code received from transmitter as screenshot
    String filename = "fooScreenshot_noReservedBits.png";
    String expectedText   = "foo";

    BufferedImage b = getResourceAndCheckNotNull(filename);
    LuminanceSource lumSrc = new BufferedImageLuminanceSource(b);

    assertNotNull("Unable to convert BufferedImage to LuminanceSrc", lumSrc);
    try {
      Result result = Receive.decode(lumSrc);
      assertEquals("Expect decoded result to match expected", expectedText, result.getText());
    } catch (NotFoundException e) {
      fail("Unable to find QR in image, "+filename + ". " + e.getMessage());
    }
  }

  /**
   * Opens a test file in resources directory as BufferedImage.
   * Can only use in testing since Android does not have BufferedImage type
   *
   * @param filename The image file that will be opened.
   */
  private BufferedImage getResourceAndCheckNotNull(String filename) {
    BufferedImage img = null;

    // Look for resource in classpath
    URL resource = UtilsTest.class.getClassLoader().getResource(filename);
    assertNotNull("Expected resource to exist: " + filename, resource);
    try {
      img = ImageReader.readImage(resource.toURI());
      assertNotNull(img);
    } catch (URISyntaxException e) {
      fail("Malformed URL: " + resource.toString());
    } catch (IOException e) {
      fail("Cannot read resource file, " + filename + "." + e.getMessage());
    }
    return img;
  }
}