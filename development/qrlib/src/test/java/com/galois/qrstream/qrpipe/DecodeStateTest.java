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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class DecodeStateTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testCapacityTooSmall() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("DecodeState must have capacity > 0");
    new DecodeState(0);
  }
  @Test
  public void testMarkDataReceivedWithIdTooSmall() {
    DecodeState s = new DecodeState(3);
    exception.expect(IndexOutOfBoundsException.class);
    exception.expectMessage("Cannot mark bit");
    s.markDataReceived(0);
  }
  @Test
  public void testMarkDataReceivedWithIdTooLarge() {
    DecodeState s = new DecodeState(3);
    exception.expect(IndexOutOfBoundsException.class);
    exception.expectMessage("Cannot mark bit");
    s.markDataReceived(3);
    s.markDataReceived(4);
  }

  @Test
  public void testMarkFailedTransmission() {
    DecodeState s = new DecodeState(1);
    assertNotEquals("State should not fail until we change it",
        State.Fail, s.getState());
    s.markFailedTransmission();
    assertEquals("State should show failure", State.Fail, s.getState());
    s.markDataReceived(1);
    assertEquals("Can never escape failure mode", State.Fail, s.getState());
  }

  @Test
  public void testMarkPartialTransmission() {
    DecodeState s = new DecodeState(2);
    assertEquals("Initial state", State.Initial, s.getState());
    s.markDataReceived(1);
    assertEquals("Intermediate state", State.Intermediate, s.getState());
  }

  @Test
  public void testMarkCompleteTransmission() {
    DecodeState s = new DecodeState(1);
    s.markDataReceived(1);
    assertEquals("Final state", State.Final, s.getState());
    s.markFailedTransmission();
    assertEquals("Shouldn't fail transmission after final state",
        State.Final, s.getState());
  }

  @Test
  public void testMissingTransmission() {
    int length = 3;
    DecodeState s = new DecodeState(length);
    assertArrayEquals("All chunks initially missing",
        new int[] {1,2,3}, s.identifyMissingChunks());
    s.markDataReceived(2);
    s.markDataReceived(3);
    assertArrayEquals("Missing first element",
        new int[] {1}, s.identifyMissingChunks());
    s = new DecodeState(length);
    s.markDataReceived(1);
    s.markDataReceived(2);
    assertArrayEquals("Missing last element",
        new int[] {3}, s.identifyMissingChunks());
    s = new DecodeState(length);
    s.markDataReceived(1);
    s.markDataReceived(3);
    assertArrayEquals("Missing middle element",
        new int[] {2}, s.identifyMissingChunks());
    s.markDataReceived(2);
    assertArrayEquals("Missing no elements",
        new int[] {}, s.identifyMissingChunks());
  }

}
