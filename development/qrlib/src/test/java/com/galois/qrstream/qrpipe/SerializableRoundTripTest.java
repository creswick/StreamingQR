package com.galois.qrstream.qrpipe;

import static com.galois.qrstream.image.ImageUtils.addBackground;
import static com.galois.qrstream.image.ImageUtils.buffToYuv;
import static com.galois.qrstream.image.ImageUtils.rotate;
import static com.galois.qrstream.image.ImageUtils.toBufferedImage;
import static com.galois.qrstream.qrpipe.TestUtils.nextNatural;
import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

import org.junit.Test;

import com.galois.qrstream.image.BitmapImage;
import com.galois.qrstream.image.YuvImage;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.zxing.DecodeHintType;

public class SerializableRoundTripTest {

  private static final int MAX_PAYLOAD_BYTES = 512;

  /**
   * The number of tests to create:
   */
  private static final int COUNT = 10;

  /**
   * The maximum percentage of failing tests that is acceptable.
   *
   * This is set based on rough estimations of the behavior of zxing at the time
   * this test was written.
   */
  private static final double FAIL_LIMIT = 0.40;

  private static final BufferedImage sampleImage;
  private static final BufferedImage whiteImage;
  private static final long seed;
  private static final Random rand;

  static {
    sampleImage = loadResourceImg("/samplePhoneImage.png");
    whiteImage = loadResourceImg("/white1500.png");
    seed = System.currentTimeMillis();
    rand = new Random(seed);
  }

  /**
   * Test object for serialization.  It's important that it implements a correct
   * equals() method, or the tests may succeed incorrectly.
   *
   * @author creswick
   *
   */
  private static class TestSerializable implements Serializable {
    private static final long serialVersionUID = -3366234227007383657L;
    private byte[] data;

    public TestSerializable(byte[] data) {
      super();
      this.data = data.clone();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(data);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TestSerializable other = (TestSerializable) obj;
      if (!Arrays.equals(data, other.data)) {
        return false;
      }
      return true;
    }
  }

  /**
   * Placeholder progress monitor for testing.
   *
   * @author creswick
   *
   */
  public static class EchoProgress implements IProgress {

    @SuppressWarnings("unused")
    private String id;

    public EchoProgress(String id) {
      this.id = id;
    }

    @Override
    public void changeState(DecodeState state) {
//      System.out.println(this.id + " " + state.getState() + ": "
//          + state.getData().cardinality()
//          +"/"+state.getCapacity());
    }
  };

  public static Collection<TestSerializable> generate(int count) {
    List<TestSerializable> data = Lists.newArrayList();

    // generate random data:
    for(int i = 0; i < count; i++ ){
      byte[] bytes = new byte[nextNatural(rand) % MAX_PAYLOAD_BYTES];
      rand.nextBytes(bytes);

      data.add(new TestSerializable(bytes));
    }

    return data;
  }

  private static BufferedImage loadResourceImg(String fileLoc) {
    InputStream imgStream =
        SerializableRoundTripTest.class.getResourceAsStream(fileLoc);
      try {
        BufferedImage img = ImageIO.read(imgStream);
        return img;
      } catch (IOException e) {
        // IllegalStateExceptions can be thrown in static initializers, but
        // IOExceptions cannot.
        throw new IllegalStateException(e);
      }
  }

  @Test
  public void testRoundTrip() {
    Receive rx  = new Receive(1500, 1500, 500, new EchoProgress("test"));
    // ZXing can incorrectly identify black blobs in image as finder
    // square and render it invalid code: https://code.google.com/p/zxing/issues/detail?id=1262
    // The suggestion was to tell ZXing when you just have QR code in image and nothing else.
    Map<DecodeHintType, Object> hints = Receive.getDecodeHints();
    hints.remove(DecodeHintType.TRY_HARDER);
    int errors = 0;
    int num = 0;

    for (TestSerializable expected : generate(COUNT)) {
      Object actual;
      try {
        actual = rx.decodeQRSerializable(new FrameManager(expected));
        if ( ! expected.equals(actual) ) {
          System.err.println("Data did not round-trip: seed="+seed+" Object count="+num);
          errors++;
        }
      } catch (Exception e) {
        System.err.println("Exception during round-trip: seed="+seed+" Object count="+num);
        System.err.println(e.getMessage());
        errors++;
      } finally {
        num++;
      }
    }

    // fail if more than FAIL_LIMIT % of the tests failed.
    assertTrue("Too many failures detected: "+errors+" out of "+num,
        errors < (num * FAIL_LIMIT));
  }

  /**
   * Placeholder CaptureFrame manager for testing.
   */
  public static class FrameManager implements ICaptureFrame {
    private final Transmit tx = new Transmit(1024, 1024);
    private final Iterable<YuvImage> yuvCodes;
    private final Iterator<YuvImage> yuvIter;

    public FrameManager(Serializable expected) throws TransmitException {
      // Build up a transformation function that will:
      //   - create a BufferedImage from a BitmapImage
      //   - superimpose that on a background image
      //   - build a YUV image from the composite.
      Function<BitmapImage, YuvImage> addImageBG = compose(
          buffToYuv, compose( addBackground(sampleImage),
                              toBufferedImage));

      Function<BitmapImage, YuvImage> addWhiteBG = compose(
          buffToYuv, compose( addBackground(whiteImage),
                              toBufferedImage));

      Function<BitmapImage, YuvImage> rotateAddBg = compose(
          buffToYuv, compose( addBackground(sampleImage),
                              compose( rotate(3), toBufferedImage)));

      // Now apply (lazily) the transformer to the incoming stream of BitmapImages
      // to get a stream of YuvImages we can extract QR codes from.
      yuvCodes = concat(
          transform(tx.encodeQRCodes(expected), addWhiteBG),
          concat(
              transform(tx.encodeQRCodes(expected), addImageBG),
              transform(tx.encodeQRCodes(expected), rotateAddBg)));
      yuvIter = yuvCodes.iterator();

    }
    @Override
    public YuvImage captureFrameFromCamera() {
      if(yuvIter.hasNext()) {
        return yuvIter.next();
      }
      return null;
    }

    @Override
    public boolean isRunning() {
      return (yuvIter != null && yuvIter.hasNext());
    }
  };
}
