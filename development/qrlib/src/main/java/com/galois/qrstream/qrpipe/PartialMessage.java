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

import java.util.List;

import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;

/**
 * Stores data from a single decoded QR code that's part of a larger sequence of QR codes.
 */
public class PartialMessage {
  private final int chunkId;
  private final int totalChunks;
  private final byte[] payload;

  /**
   * Initialize partial message with its data and sequence information.
   * @param chunkId Unique number identifying this chunk of data within a sequence.
   * @param totalChunks The number of chunks in a sequence of transmitted data.
   * @param payload The partial message contains within the QR code.
   */
  private PartialMessage(int chunkId, int totalChunks, byte[] payload) {
    if (chunkId < 1 || totalChunks < 1) {
      throw new IllegalArgumentException("Expected message to have positive chunk inputs.");
    }else if (payload == null) {
      throw new NullPointerException("Invalid input for msg.");
    }
    this.chunkId = chunkId;
    this.totalChunks = totalChunks;
    this.payload = payload.clone();
  }

  public int getTotalChunks() {
    return totalChunks;
  }

  public int getChunkId() {
    return chunkId;
  }

  public byte[] getPayload() {
    return payload.clone();
  }

  /**
   * Extracts information about the message within the decoded QR code
   * and initialize a new {@code PartialMessage} with that information.
   * It retrieves, its position in data transmission ({@code chunkId}),
   * the total number of chunks in a sequence of transmitted data
   * ({@code totalChunks}), and the partial message contained within
   * the QR {@code payload}.
   * @param decodedQR The result from decoding a QR code within an image.
   * @throws ReceiveException If the decoded QR code has an invalid format.
   */
  public static PartialMessage createFromResult (Result decodedQR) throws ReceiveException {
    final int chunkId;
    final int totalChunks;
    final byte[] payload;

    byte[] message = getRawData(decodedQR);
    if (message == null || message.length < Utils.getNumberOfReservedBytes()) {
      throw new ReceiveException("QR code is missing sequence data.");
    }

    try {
      chunkId = Utils.extractChunkId(message);
      totalChunks = Utils.extractTotalNumberChunks(message);
      payload = Utils.extractPayload(message);
    }catch (IllegalArgumentException e) {
      throw new ReceiveException("QR code is illformed." + e.getMessage());
    }
    if (chunkId > totalChunks) {
      throw new ReceiveException("QR code is illformed, chunkId="+chunkId
          + " > totalChunks=" + totalChunks);
    }

    return new PartialMessage(chunkId,totalChunks,payload);
  }

  /**
   * Extract raw bytes from decoded QR code.
   * @throws AssertionError if ZXing library returned more than one array
   */
  protected static byte[] getRawData(final Result decodedQR) {
    byte[] rawBytes = new byte[0];

    @SuppressWarnings("unchecked")
    List<byte[]> dataSegments = (List<byte[]>) decodedQR.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
    if (!dataSegments.isEmpty()) {
      // I'm not sure why dataSegments would have more than one entry.
      if (dataSegments.size() > 1) {
        System.err.println("Decoded result has "+dataSegments.size()+" elements. We expected just one.");
        throw new AssertionError();
      }
      rawBytes = dataSegments.get(0);
    }
    return rawBytes;
  }
}
