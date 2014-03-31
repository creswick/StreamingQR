package com.galois.qrstream.qrpipe;


import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

@Ignore("Ignore until OutOfMemory exception is resolved") @RunWith(Parameterized.class)
public class RandomQRDecodeTest {

  /**
   * The number of qr codes to create:
   */
  private static final int COUNT = 30;

  /**
   * Null-op progress monitor.
   */
  public static final IProgress NULL_PROGRESS = new IProgress() {
    @Override
    public void changeState(DecodeState state) {
    }};

  private static int nextNatural(Random r) {
    return Math.abs(r.nextInt());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> setup() {
    List<Object[]> qrcodes = Lists.newArrayList();

    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    for(int i = 0; i < COUNT; i++ ){
      byte[] bytes = new byte[nextNatural(rand) % 2048];
      rand.nextBytes(bytes);
      int height = 200 + (nextNatural(rand) % 7) * 100;
      int width = 200 + (nextNatural(rand) % 7) * 100;

      Transmit t = new Transmit(height, width);
      BitMatrix bmap;

      try {
        bmap = t.bytesToQRCode(bytes);
      } catch (TransmitException e) {
        continue;
      }

      BufferedImage newCode = MatrixToImageWriter.toBufferedImage(bmap);

      qrcodes.add(new Object[] { "seed: "+ seed + ": "+i
                               , newCode
      });
    }
    return qrcodes;
  }

  private final BufferedImage qrcode;

  public RandomQRDecodeTest(String name, BufferedImage qrcode) {
    super();
    this.qrcode = qrcode;
  }

  @Test
  public void testDecodeRandomQR() throws ReceiveException, IOException {
    try {
      QRDecodeRegressionTest.decodeImage(qrcode);
    } catch (Exception e) {
      File outputFile =  File.createTempFile("QR-code", ".png");
      ImageIO.write(qrcode, "png", outputFile);

      e.printStackTrace();
      fail("Decode failed due to exception; qr code saved to: "+outputFile);
    }
  }

}
