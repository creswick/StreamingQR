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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
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

/**
 * Property-based tests to augment the specific boundary tests in UtilsTest.java.
 * @author creswick
 *
 */
@RunWith(Parameterized.class)
public class NumsToByteTest {
  private static final int COUNT = 100;

  @Parameters(name = "{0}")
  public static Collection<Object[]> setup() {
    List<Object[]> vals = Lists.newArrayList();

    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    for(int i = 0; i < COUNT; i++ ){
      
      vals.add(new Object[] { rand.nextInt() });
    }
    return vals;
  }

  private int val;

  public NumsToByteTest(int s) {
    this.val = s;
  }
  
  /**
   * Test that values round-trip through byte arrays and back.
   */
  @Test
  public void testIntByteArrayRoundTrip() {
    assumeTrue("Utils.intToBytes does not support negative values.", val >= 0);

    int actual = Utils.bytesToInt(Utils.intToBytes(val));
    
    assertEquals("Round trip failed", val, actual);
  }

  /**
   * Test that short values round-trip through Streams and back.
   */
  @Test
  public void testShortStreamRoundTrip()
      throws IllegalArgumentException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    short shortVal = (short)this.val;
    
    StreamUtils.writeShort(baos, shortVal);
    byte[] buf = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    short actual = StreamUtils.readShort(bais);
    
    assertEquals("Round trip failed", shortVal, actual); 
  }

  /**
   * Test that short values round-trip through Streams and back.
   */
  @Test
  public void testIntStreamRoundTrip()
      throws IllegalArgumentException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    StreamUtils.writeInt(baos, val);
    byte[] buf = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    int actual = StreamUtils.readInt(bais);
    
    assertEquals("Round trip failed", val, actual); 
  }
}
