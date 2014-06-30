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
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;

/**
 * Stores data from a single decoded QR code that's part of a larger sequence of QR codes.
 */
public final class PartialMessage {
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
   *
   * If the decoded QR code is not properly formatted streaming QR code
   * then return {@code null} PartialMessage. We do not throw exceptions
   * anymore since the upstream code handles the {@code null} message.
   *
   * @param decodedQR The result from decoding a QR code within an image.
   * @param maxChunks The maximum number of chunks expected for a QR stream.
   */
  protected static PartialMessage createFromResult(Result decodedQR, int maxChunks) {
    final int chunkId;
    final int totalChunks;
    final byte[] payload;

    // Check that the QR code has enough bytes for extracting
    // the sequence data needed to identify it as streaming QR.
    byte[] message = getRawData(decodedQR);
    if (message == null || message.length < Utils.getNumberOfReservedBytes()) {
      return null;
    }

    // Check that the extracted sequence data is well formed.
    try {
      chunkId = Utils.extractChunkId(message);
      totalChunks = Utils.extractTotalNumberChunks(message);
      payload = Utils.extractPayload(message);
    }catch (IllegalArgumentException e) {
      return null;
    }

    // Ensure positive chunk data
    if (chunkId < 1 || totalChunks < 1) {
      return null;
    }

    // Can only stream the QR code if its total chunks is less than threshold.
    // This is necessary since a BitVector is used to track the chunks
    // we've seen and it cannot get too large without running out of memory.
    if (totalChunks > maxChunks) {
      return null;
    }

    // Properly formatted streaming QR code must have all chunkIds
    // less than or equal to the expected totalChunks encoded with the QR code.
    if (chunkId > totalChunks) {
      return null;
    }

    return new PartialMessage(chunkId,totalChunks,payload);
  }

  /**
   * Extract raw bytes from decoded QR code.  If none can be extracted,
   * then this method returns {@code null}.
   */
  private static byte[] getRawData(final Result decodedQR) {

    // Whenever the ZXing result metadata is missing or does not
    // have the BYTE_SEGMENTS object return null to indicate error.
    // Might happen if we're trying to read a non-streaming type of QR code.
    Map<ResultMetadataType,?> meta = decodedQR.getResultMetadata();
    if (meta == null || !meta.containsKey(ResultMetadataType.BYTE_SEGMENTS)) {
      return null;
    }

    ImmutableList.Builder<Byte> msgBuilder = new ImmutableList.Builder<Byte>();

    @SuppressWarnings("unchecked")
    List<byte[]> dataSegments = (List<byte[]>) meta.get(ResultMetadataType.BYTE_SEGMENTS);
    for (byte[] bs : dataSegments) {
      msgBuilder.addAll(Bytes.asList(bs));
    }
    return Bytes.toArray(msgBuilder.build());
  }
}
