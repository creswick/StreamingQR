package com.galois.qrstream.qrpipe;


import static org.junit.Assert.fail;
import static org.junit.Assert.assertArrayEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.galois.qrstream.image.YuvImage;
import com.google.common.collect.Lists;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

@RunWith(Parameterized.class)
public class RandomQRDecodeTest {

  /**
   * The number of qr codes to create:
   */
  private static final int COUNT = 10;
  
  /**
   * Null-op progress monitor.
   */
  public static final IProgress NULL_PROGRESS = new IProgress() {
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
      int height = 500; //nextNatural(rand) % 1024;
      int width = 500; //nextNatural(rand) % 1024;
      
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
                               , bytes 
                               , height
                               , width
      });
    }
    return qrcodes;
  }
  
  private final String name;
  private final BufferedImage qrcode;
  private final byte[] oracle;
  private final int width;
  private final int height;

  public RandomQRDecodeTest(String name, BufferedImage qrcode, byte[] oracle,
      int width, int height) {
    super();
    this.name = name;
    this.qrcode = qrcode;
    this.oracle = Arrays.copyOf(oracle, oracle.length);
    this.width = width;
    this.height = height;
  }
  
  @Test
  public void testDecodeRandomQR() throws ReceiveException, IOException {
    byte[] actual;
    try {
      Receive r = new Receive(height, width, 500, NULL_PROGRESS);
      
      BlockingQueue<YuvImage> queue = new ArrayBlockingQueue<YuvImage>(2);
      queue.add(new YuvImage(YuvUtilities.toYUV(qrcode), width, height));
      actual = r.decodeQRCodes(queue);
      assertArrayEquals(name + ": decode failed", oracle, actual);
    } finally {
      File outputFile =  File.createTempFile("QR-code", ".png");
      ImageIO.write(qrcode, "png", outputFile);
      
      fail("Decode failed due to exception; qr code saved to: "+outputFile);
    }
  }
  
}
