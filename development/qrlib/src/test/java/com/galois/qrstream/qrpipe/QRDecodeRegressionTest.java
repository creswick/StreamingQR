/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.qrpipe;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
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

    // If this does not throw an exception, everything is fine:
    decodeImage(bi);
  }

  /**
   * Decode a qr code from a buffered image.
   *
   * Catches transmission failure exceptions, and returns null.
   *
   * @param bi The buffered image to decode.
   * @return
   * @throws ReceiveException
   */
  public static byte[] decodeImage(BufferedImage bi) throws ReceiveException {
    int width = bi.getWidth();
    int height = bi.getHeight();

    YuvImage img = new YuvImage(YuvUtilities.toYUV(bi),
                                width, height);

    Receive receive = new Receive(height, width, 100,
        RandomQRDecodeTest.NULL_PROGRESS);

    byte[] result = null;
    try {
      result = receive.decodeQRCodes(new EchoFrame(img));
    } catch (ReceiveException e) {
      // this should only happen if receive was expecting more than one QR code:
      if (!e.getMessage().startsWith("Transmission failed")) {
        throw e;
      }
    }

    return result;
  }

  /**
   * Placeholder CaptureFrame manager for testing.
   */
  public static class EchoFrame implements ICaptureFrame {
    private final YuvImage img;
    private boolean running = true;
    public EchoFrame(YuvImage img) {
      this.img = img;
    }

    // Allows image to be decoded only once before exiting
    @Override
    public YuvImage captureFrameFromCamera() {
      running = false;
      return this.img;
    }

    @Override
    public boolean isRunning() {
      return running;
    }
  };

}
