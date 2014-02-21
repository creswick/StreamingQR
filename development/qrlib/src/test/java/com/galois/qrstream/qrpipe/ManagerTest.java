package com.galois.qrstream.qrpipe;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class ManagerTest {

  @BeforeClass
  public static void testSetup() {}

  @AfterClass
  public static void testCleanup() {}

  @Test
  public void testSquareX() {
    Manager tester = new Manager();
    assertEquals ("4 ^ 2 must be 16", 16, tester.squareX(4));
  }

}
