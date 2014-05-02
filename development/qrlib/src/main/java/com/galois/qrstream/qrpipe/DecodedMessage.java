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

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Stores message from sequence of decoded QR codes. Note, the initial
 * capacity is unknown until first QR code is read.
 */
public class DecodedMessage {
  // Container for saving received data.
  // Using SortedMap so that message can be assembled in order.
  private final SortedMap<Integer, PartialMessage> receivedData;

  // Track progress of decoding
  private final IProgress decodeProgress;
  private DecodeState decodeState;

  public DecodedMessage (IProgress progress) {
    // Initialize 'decodeState' upon decoding first QR code.
    receivedData = new TreeMap<Integer, PartialMessage>();
    decodeProgress = progress;
  }

  /**
   * Returns the whole transmitted message whenever it is available, otherwise
   * it returns an empty message to indicate only partial message received.
   */
  public byte[] getEntireMessage() {
    if (receivedData.isEmpty() || (decodeState.getState() != State.Final)) {
      return new byte[0];
    }

    // Assemble message in order, we assume key are sorted
    ByteArrayDataOutput bstream = ByteStreams.newDataOutput();
    for(Entry<Integer, PartialMessage> entry : receivedData.entrySet()) {
      bstream.write(entry.getValue().getPayload());
    }
    return bstream.toByteArray();
  }

  /**
   * Mark transmission failure. Expect no more QR codes to decode.
   */
  public void setFailedDecoding() {
    DecodeState failed;

    // Possible for transmission to fail before decodeState is initialized.
    if (decodeState == null) {
      failed = new DecodeState(1);
    }else{
      failed = decodeState;
    }
    failed.markFailedTransmission();
    decodeProgress.changeState(failed);
  }

  /**
   * Mark progress of data transmission by setting the {@code chunkId}
   * bit in {@code DecodeState} to true whenever a QR code has been decoded.
   * It also sets up the initial sizes of {@code receivedData} if this is
   * the first QR code encountered.
   * @return The {@code State} indicating whether the whole message has been received.
   */
  public State saveMessageChunk(PartialMessage msgPart) {

    // Set up message container if this is the first QR code encountered.
    if (decodeState == null) {
      decodeState = new DecodeState(msgPart.getTotalChunks());
      receivedData.clear();
    }
    // Save message part if we haven't seen it already.
    if (!receivedData.containsKey(msgPart.getChunkId())) {
      receivedData.put(msgPart.getChunkId(), msgPart);
      decodeState.markDataReceived(msgPart.getChunkId());
      decodeProgress.changeState(decodeState);
      System.out.println("saveMessageChunk: Saving chunk " + msgPart.getChunkId() + " of " + msgPart.getTotalChunks());
    }else{
      System.out.println("saveMessageChunk: Already saved chunk " + msgPart.getChunkId() + " of " + msgPart.getTotalChunks());
    }
    return decodeState.getState();
  }
}
