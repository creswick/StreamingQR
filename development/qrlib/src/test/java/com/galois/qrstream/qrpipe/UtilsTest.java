package com.galois.qrstream.qrpipe;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UtilsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testBytesToIntConversions() {
    // Check that we padded smaller inputs correctly
    try {
      assertEquals("Expect 0 = 0x00", 0,
          Utils.bytesToInt(new byte[] { 0x00 }));
      assertEquals("Expect 1 = 0x01", 1,
          Utils.bytesToInt(new byte[] { 0x01 }));
      assertEquals("Expect 10,000 = 0x2710", 10000,
          Utils.bytesToInt(new byte[] { 0x27, 0x10 }));
      assertEquals("Expect 98,048 = 0x017f00", 98048,
          Utils.bytesToInt(new byte[] { 0x01, 0x7f, 0x00 }));
      assertEquals("Expect 655,360,018 = 0x27100012", 655360018,
          Utils.bytesToInt(new byte[] { 0x27, 0x10, 0x00, 0x12 }));
    } catch (IllegalArgumentException e){
      System.out.println("Do not fail test when IllegalArgumentException "+
          "thrown. In this case, it will only occur when NUM_BYTES_PER_INT < 4");
    }
  }

  @Test
  public void testBytesToIntRoundTripConversion() {
    byte[] a = new byte[] {0x27,0x10,0x00,0x12};
    int b = 655360018;

    assertEquals("Expect 1 = bytesToInt(intToBytes(1))", 1,
        Utils.bytesToInt(Utils.intToBytes(1)));
    try {
      assertEquals("Expect x = bytesToInt(intToBytes(x))", b,
          Utils.bytesToInt(Utils.intToBytes(b)));
      assertArrayEquals("Expect x = intToBytes(BytesToInt(x))", a,
          Utils.intToBytes(Utils.bytesToInt(a)));

      byte[] bytesMax = Utils.intToBytes(Integer.MAX_VALUE);
      assertEquals("Expect Integer.MAX_VALUE = bytesToInt(intToBytes(MAX_VALUE))",
          Integer.MAX_VALUE, Utils.bytesToInt(bytesMax));
    } catch (IllegalArgumentException e){
      System.out.println("Do not fail test when IllegalArgumentException "+
          "thrown. In this case, it will only occur when NUM_BYTES_PER_INT < 4");
    }
  }

  @Test
  public void testBytesToIntWithNegativeInput() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot convert negative numbers.");
    // Negative integers are not converted
    byte[] negOne = new byte[] { (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff };
    Utils.bytesToInt(negOne);
  }

  @Test
  public void testBytesToIntWithInputTooLarge() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Byte array too large.");
    Utils.bytesToInt(new byte[] { 0x01, 0x01, 0x01, 0x01, 0x01});
  }

  @Test
  public void testIntToBytesConversion() {
    // Check positive ints convert to byte[]
    assertArrayEquals("Expect 0 = 0x00000000",
        new byte[] {0x00,0x00,0x00,0x00}, Utils.intToBytes(0));
    assertArrayEquals("Expect 98,048 = 0x00017f00",
        new byte[] { 0x00, 0x01, 0x7f, 0x00 },
        Utils.intToBytes(98048));
  }

  @Test
  public void testIntToBytesWithNegativeInput() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot convert negative numbers.");
    // Negative integers are not converted
    Utils.intToBytes(-1);
  }

  @Ignore("Enable if NUM_BYTES_PER_INT < 4") @Test
  public void testIntToBytesWithInputTooLarge() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Byte array too large.");
    Utils.intToBytes(Integer.MAX_VALUE);
  }
}
