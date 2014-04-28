package com.galois.qrstream.qrpipe;

import static com.galois.qrstream.image.ImageUtils.addBackground;
import static com.galois.qrstream.image.ImageUtils.buffToYuv;
import static com.galois.qrstream.image.ImageUtils.rotate;
import static com.galois.qrstream.image.ImageUtils.toBufferedImage;
import static com.galois.qrstream.qrpipe.TestUtils.nextNatural;
import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.galois.qrstream.image.BitmapImage;
import com.galois.qrstream.image.YuvImage;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.zxing.DecodeHintType;

@RunWith(Parameterized.class)
public class EncodeSerializableTest {

  /**
   * The number of tests to create:
   */
  private static final int COUNT = 40;
  
  private static final BufferedImage sampleImage;
  private static final BufferedImage whiteImage;
  
  static {
    sampleImage = loadResourceImg("/samplePhoneImage.png");
    whiteImage = loadResourceImg("/white1500.png");
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
      this.data = data;
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
  
  public static class EchoProgress implements IProgress {
    
    private String id;
    
    public EchoProgress(String id) {
      this.id = id;
    }
    
    @Override
    public void changeState(DecodeState state) {
      System.out.println(this.id + " " + state.getState() + ": "
          + state.getData().cardinality()
          +"/"+state.getCapacity());
    }
  };
  
  @Parameters(name = "{0}")
  public static Collection<Object[]> setup() {
    List<Object[]> data = Lists.newArrayList();

    // generate random data:
    long seed = System.currentTimeMillis();
    //long seed = 1398390654817L; // test 1, 4, 8, 9 fail.
    //long seed = 1398438575853L; // test 9, code 4 fails.
    Random rand = new Random(seed);
    
    for(int i = 0; i < COUNT; i++ ){
      byte[] bytes = new byte[nextNatural(rand) % 1024];
      rand.nextBytes(bytes);

      data.add(new Object[]{ "seed: "+ seed + ": "+i + " len: "+bytes.length
                           , new TestSerializable(bytes)
                           });
    }
    
    return data;
  }

  private static BufferedImage loadResourceImg(String fileLoc) {
    InputStream imgStream =
        EncodeSerializableTest.class.getResourceAsStream(fileLoc);  
      try {
        BufferedImage img = ImageIO.read(imgStream);
        return img;
      } catch (IOException e) {
        // IllegalStateExceptions can be thrown in static initializers, but
        // IOExceptions cannot.
        throw new IllegalStateException(e);
      }
  }

  private Serializable expected;
  private String name;
  
  public EncodeSerializableTest(String name, Serializable obj) {
    this.name = name;
    this.expected = obj;
  }
  
  @Test
  public void test() throws TransmitException, ReceiveException, IOException {
    Receive rx  = new Receive(1500, 1500, 500, new EchoProgress(name));
    // ZXing can incorrectly identify black blobs in image as finder
    // square and render it invalid code: https://code.google.com/p/zxing/issues/detail?id=1262
    // The suggestion was to tell ZXing when you just have QR code in image and nothing else.
    Map<DecodeHintType, Object> hints = Receive.getDecodeHints();
    //hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    hints.remove(DecodeHintType.TRY_HARDER);
    
    Object actual = null;
    try {
      hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
      actual = rx.decodeQRSerializable(encode(expected));
    }catch (ReceiveException e) {
      System.err.println("Could not decode with PURE_BARCODE=TRUE");
      hints.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
      try {
        actual = rx.decodeQRSerializable(encode(expected));
      } catch (ReceiveException e1) {
        System.err.println("Could not decode with PURE_BARCODE=FALSE");
      }
      throw e;
    }
    assertEquals("Round-trip failed.", expected, actual);    
  }

  private BlockingQueue<YuvImage> encode(Serializable expected)
      throws TransmitException {
    Transmit tx = new Transmit(1024, 1024);
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
    
//    Iterable<BufferedImage> imgs = transform(tx.encodeQRCodes(expected),
//        compose( addBackground(sampleImage), compose( rotate(3), toBufferedImage)));
//    int i = 1;
//    for (BufferedImage img : imgs) {
//      ImageIO.write(img, "PNG", new File("/Users/creswick/tmp/testImg/"+i+".png"));
//      i++;
//    }
//    
    // Now apply (lazily) the transformer to the incoming stream of BitmapImages
    // to get a stream of YuvImages we can extract QR codes from.
    Iterable<YuvImage> yuvCodes = concat(
        transform(tx.encodeQRCodes(expected), addWhiteBG),
        concat(
            transform(tx.encodeQRCodes(expected), addImageBG),
            transform(tx.encodeQRCodes(expected), rotateAddBg)));
    
    // Include the unchanged qr codes:
    BlockingQueue<YuvImage> yuvQueue = Queues.newLinkedBlockingQueue(yuvCodes);
    return yuvQueue;
  }
}
