package com.galois.qrstream.qrpipe;

import java.util.BitSet;

/**
 * Used to mark the progress of decoding stream of QR codes.
 */
public class DecodeState {

	private final BitSet data;
	private final int capacity;

	// True if transmission of QR codes stops before entire message received
	private boolean hasTransmissionFailed;

	/**
	 * Initialize DecodeState with the number of QR codes that it expects
	 * in the stream. The caller should have decoded at least one QR code to
	 * know how many QR codes it should expect to receive and pass that
	 * number to this constructor.
	 * @param capacity The number of QR code chunks in a full message.
	 */
	public DecodeState(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("DecodeState must have capacity > 0");
		}
		this.capacity = capacity;
		this.data = new BitSet(capacity);
		this.hasTransmissionFailed = false;
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
		} else if (data.cardinality() == this.capacity) {
			return State.Final;
		} else {
			return State.Intermediate;
		}
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
			throw new IndexOutOfBoundsException("Cannot mark bit the  chunkId: " + chunkId +
			                                    ", is out of bounds");
		}
		data.set(chunkId - 1);
	}

	/**
	 * Indicate that transmission failed and we expect no more QR codes
	 * to decode.
	 */
	public void markFailedTransmission() {
		this.hasTransmissionFailed = true;
	}

	/**
	 * Get a deep copy of the underlying bitset.
	 *
	 * @return The bitset underlying the decode state.
	 */
	public BitSet getData() {
		return (BitSet) data.clone();
	}
}
