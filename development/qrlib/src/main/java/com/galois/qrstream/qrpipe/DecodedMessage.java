package com.galois.qrstream.qrpipe;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Stores message from sequence of decoded QR codes. Note, the initial
 * capacity is unknown until first QR code is read.
 */
public class DecodedMessage {
  // Container for saving received data.
  // Using SortedMap so that message can be assembled in order.
  private final SortedMap<Integer, byte[]> receivedData;

  // Track progress of decoding
  private final IProgress decodeProgress;
  private DecodeState decodeState;

  public DecodedMessage (IProgress progress) {
    // Initialize 'decodeState' upon decoding first QR code.
    receivedData = new TreeMap<Integer, byte[]>();
    decodeProgress = progress;
  }

  /**
   * Mark progress of data transmission by setting the {@code chunkId}
   * bit in {@code DecodeState} to true whenever a QR code has been decoded.
   * It also sets up the initial sizes of {@code receivedData} if this is
   * the first QR code encountered.
   */
  public void saveMessageChunk(int chunkId, int totalChunks, byte[] msg) {
    if (totalChunks < 1 || chunkId < 1) {
      throw new IllegalArgumentException("Expected positive chunk inputs.");
    }else if (msg == null) {
      throw new NullPointerException("Invalid input for msg.");
    }
    // Set up message container if this is the first QR code encountered.
    if (decodeState == null) {
      decodeState = new DecodeState(totalChunks);
      receivedData.clear();
    }
    // Save message part if we haven't seen it already.
    if (!receivedData.containsKey(chunkId)) {
      receivedData.put(chunkId, msg.clone());
      decodeState.markDataReceived(chunkId);
      decodeProgress.changeState(decodeState);
    }else{
    }
  }
}
