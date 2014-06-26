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
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;

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
   * 
   * @param decodedQR The result from decoding a QR code within an image.
   * @param maxChunks The maximum number of chunks expected for a QR stream.
   * @throws ReceiveException If the decoded QR code has an invalid format.
   */
  protected static PartialMessage createFromResult(Result decodedQR, int maxChunks) {
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
    
    if (totalChunks > maxChunks) {
      throw new ReceiveException("QR code is illformed. "
          + "Too many chunks expected.");
    }
    
    if (chunkId > totalChunks) {
      throw new ReceiveException("QR code is illformed, chunkId="+chunkId
          + " > totalChunks=" + totalChunks);
    }

    try {
      PartialMessage pm = new PartialMessage(chunkId,totalChunks,payload);
      return pm;
    }catch(Exception e){
      throw new ReceiveException(e);
    }
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

    // Expect any streaming QR codes to have this type
    // All other qr codes have to be handled in a different way
    ParsedResult result = ResultParser.parseResult(decodedQR);
    if (result.getType() != ParsedResultType.TEXT) {
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
