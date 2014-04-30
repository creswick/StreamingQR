package com.galois.qrstream.qrpipe;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class ShortToByteTest {
  private static final int COUNT = 100;

  @Parameters(name = "{0}")
  public static Collection<Object[]> setup() {
    List<Object[]> shorts = Lists.newArrayList();

    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    for(int i = 0; i < COUNT; i++ ){
      
      shorts.add(new Object[] { ((short)rand.nextInt()) });
    }
    return shorts;
  }

  private short val;

  public ShortToByteTest(short s) {
    this.val = s;
  }
  
  /**
   * Test that short values round-trip through byte arrays and back.
   */
  @Test
  public void testByteArrayRoundTrip() {
    short actual = Utils.bytesToShort(Utils.shortToBytes(val));
    
    assertEquals("Round trip failed", val, actual);
  }

  /**
   * Test that short values round-trip through Streams and back.
   */
  @Test
  public void testStreamRoundTrip() throws IllegalArgumentException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    StreamUtils.writeShort(baos, val);
    byte[] buf = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    short actual = StreamUtils.readShort(bais);
    
    assertEquals("Round trip failed", val, actual); 
  }
  
}
