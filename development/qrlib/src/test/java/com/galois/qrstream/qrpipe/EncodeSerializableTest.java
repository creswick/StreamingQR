package com.galois.qrstream.qrpipe;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.galois.qrstream.image.BitmapImage;
import com.galois.qrstream.image.ImageUtils;
import com.google.common.collect.Lists;

import static com.galois.qrstream.qrpipe.TestUtils.nextNatural;

@RunWith(Parameterized.class)
public class EncodeSerializableTest {

  /**
   * The number of tests to create:
   */
  private static final int COUNT = 10;
  
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
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TestSerializable other = (TestSerializable) obj;
      if (!Arrays.equals(data, other.data))
        return false;
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
    Random rand = new Random(seed);
    
    for(int i = 0; i < COUNT; i++ ){
      byte[] bytes = new byte[nextNatural(rand) % 2048];
      rand.nextBytes(bytes);

      data.add(new Object[]{ "seed: "+ seed + ": "+i + " len: "+bytes.length
                           , new TestSerializable(bytes)
                           });
    }
    
    return data;
  }

  private Serializable expected;
  private String name;
  
  public EncodeSerializableTest(String name, Serializable obj) {
    this.name = name;
    this.expected = obj;
  }
  
  @Test
  public void test() throws TransmitException, ReceiveException {
    Transmit tx = new Transmit(1024, 1024);
    Receive rx  = new Receive(1024, 1024, 10000, new EchoProgress(name));
    
    Iterable<BitmapImage> codes = tx.encodeQRCodes(expected);
    Object acutal = rx.decodeQRSerializable(ImageUtils.toYuvQueue(codes));
    
    assertEquals("Round-trip failed.", expected, acutal);
  }
}
