package com.galois.qrstream.qrpipe;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.galois.qrstream.image.YuvImage;
import com.google.common.collect.Lists;


@RunWith(Parameterized.class)
public class QRDecodeRegressionTest {

  private static final FilenameFilter PNG_FILTER = new FilenameFilter(){
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith("png");
    }
  };

  @Parameters(name = "{0}")
  public static Collection<Object[]> setup() {
    List<Object[]> qrcodes = Lists.newArrayList();

    File testDir = new File("src/test/resources/troublesomeQRCodes");

    for (File file : testDir.listFiles(PNG_FILTER)) {
      qrcodes.add(new Object[]{ file });
    }

    return qrcodes;
  }

  private final File file;

  public QRDecodeRegressionTest(File file) {
    super();
    this.file = file;
  }

  @Test
  public void testDecode() throws IOException, ReceiveException {
    BufferedImage bi = ImageIO.read(file);

    int width = bi.getWidth();
    int height = bi.getHeight();

    YuvImage img = new YuvImage(YuvUtilities.toYUV(bi),
                                width, height);

    BlockingQueue<YuvImage> queue = new ArrayBlockingQueue<YuvImage>(2);
    queue.add(img);

    Receive receive = new Receive(height, width, 200,
        RandomQRDecodeTest.NULL_PROGRESS);

    try {
      byte[] result = receive.decodeQRCodes(queue);
      // just to be sure we got a result:
      assertNotNull("result was null for some reason.", result);
    } catch (ReceiveException e) {
      // this should only happen if receive was expecting more than one QR code:

      if (! e.getMessage().startsWith("Transmission failed")) {
        e.printStackTrace();
        fail("Wrong Receive Exception: "+e);
      }
    }
  }
}
