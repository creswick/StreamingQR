package com.galois.qrstream.qrpipe;

import java.text.NumberFormat;
import java.util.BitSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

/**
 * Used to mark the progress of decoding stream of QR codes.
 */
public class DecodeState {

	private final BitSet data;
	private final int capacity;

	// True if transmission of QR codes stops before entire message received
	private boolean hasTransmissionFailed;

	// True if encountered frame without QR code or with one that couldn't be read
	// Indicates to receiver that it needs another frame.
	private boolean hasFrameFailed;

	/**
	 * Initialize DecodeState with the number of QR codes that it expects
	 * in the stream. The caller should have decoded at least one QR code to
	 * know how many QR codes it should expect to receive and pass that
	 * number to this constructor.
	 * @param capacity The number of QR code chunks in a full message.
	 */
	public DecodeState(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("DecodeState must have capacity > 0");
		}
		this.capacity = capacity;
		this.data = new BitSet(capacity);
		this.hasTransmissionFailed = false;
		this.hasFrameFailed = false;
	}

	/**
	 * Return the state that a transmission is in. If no QR codes have been
	 * received and decoded, this returns the {@code Initial} state. When at
	 * least one QR code has been decoded, return {@code Intermediate} state.
	 * Finally, when all QR codes have been received and decoded, the state
	 * transitions to {@code Final}.
	 */
	public State getState () {
		if (hasTransmissionFailed) {
			return State.Fail;
		} else if (data.isEmpty()) {
			return State.Initial;
		} else if (allBitsSet()) {
			return State.Final;
		} else {
			return State.Intermediate;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(getState().toString());
		int nFramesReceived = data.cardinality();
		double complete = (double) nFramesReceived / this.capacity;
		if(nFramesReceived > 0) {
		  s.append(", " + nFramesReceived +" of " + this.capacity + ": ");
			s.append(NumberFormat.getPercentInstance().format(complete));
		}
		return s.toString();
	}

	/**
	 * Mark progress of data transmission by setting the bit corresponding
	 * to {@code chunkId} of the underlying bitset to true.
	 *
	 * @throw IndexOutOfBoundsException if {@code chunkId} is not within bounds
	 * of capacity of underlying bitset.
	 */
	public void markDataReceived (int chunkId) throws IndexOutOfBoundsException {
		if (chunkId < 1 || chunkId > capacity) {
			throw new IndexOutOfBoundsException("Cannot mark bit, the chunkId: " + chunkId +
			                                    ", is out of bounds");
		}
		data.set(chunkId - 1);
		// Reset failed frame tag if it was set because this method
		// indicates successful QR code reading
		if (hasFrameFailed) {
		  hasFrameFailed = false;
		}
	}

	/**
	 * Indicate that transmission failed and we expect no more QR codes
	 * to decode.
	 */
	public void markFailedTransmission() {
		// Fail only if transmission didn't already complete successfully.
		if (!allBitsSet()) {
			this.hasTransmissionFailed = true;
		}
	}

	/**
   * Indicate that frame failed and we expect more QR codes
   * to decode.
   */
  public void markFailedFrame() {
    if (!allBitsSet()) {
      this.hasFrameFailed = true;
    }
  }

	/**
	 * Get a deep copy of the underlying bitset.
	 *
	 * @return The bitset underlying the decode state.
	 */
	public BitSet getData() {
		return (BitSet) data.clone();
	}

	/**
	 * Returns a list of integers identifying the chunks of data that are still
	 * missing from the transmission.
	 */
	public int[] identifyMissingChunks() {
		List<Integer> missingChunks = Lists.newArrayList();
		int i = 0;
		do {
			int bit = data.nextClearBit(i);
			if (bit < capacity) {
				missingChunks.add(bit + 1);
			}
			i = bit + 1;
		} while (i < capacity);
		return Ints.toArray(missingChunks);
	}

	/**
	 * Returns the capacity
	 */
	public int getCapacity() {
    // BitSet capacity is rounded up to nearest 64, that's why
    // we do not want to return data.getCapacity()
		return this.capacity;
	}

	/**
	 * Returns the number of QR codes that have been received and decoded
	 */
	public int getTotalFramesDecoded() {
		//Note, this call will take O(n) time.
		return data.cardinality();
	}


	/**
	 * Returns true when all of the chunks of data have been
	 * received and false otherwise.
	 */
	private boolean allBitsSet() {
		return (data.cardinality() == this.capacity);
	}

}
