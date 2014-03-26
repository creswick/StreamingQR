package com.galois.qrstream.qrpipe;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores message from sequence of decoded QR codes. Note, the initial
 * capacity is unknown until first QR code is read.
 */
public class DecodedMessage {
  /* Container for saving received data */
  private Map<Integer, byte[]> receivedData;

  /* Track progress of decoding */
  private final DecodeState decodeState;
  private final IProgress decodeProgress;

  private static final boolean DEBUG = false;

  public DecodedMessage (IProgress progress) {
    receivedData = null;
    decodeState = new DecodeState();
    decodeProgress = progress;
    decodeProgress.changeState(decodeState);
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
    // Could have initialized before knowing capacity but then
    // it's likely map would need to be resized.
    if (receivedData == null) {
      receivedData = new HashMap<Integer, byte[]>(totalChunks);
      decodeState.setInitialCapacity(totalChunks);
      if(DEBUG) {
        System.out.println("saveMessageChunk: initialize to size " + totalChunks);
      }
    }
    // Save message part if we haven't seen it already.
    if (!receivedData.containsKey(chunkId)) {
      decodeState.markDataReceived(chunkId);
      receivedData.put(chunkId, msg.clone());
      decodeProgress.changeState(decodeState);
      if(DEBUG) {
        System.out.println("saveMessageChunk: updated state to " + decodeState.getState());
      }
    }else{
      if(DEBUG) {
        System.out.println("saveMessageChunk: nothing to do - already recorded chunk");
      }
    }
  }

}
