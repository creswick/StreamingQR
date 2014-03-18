package com.galois.qrstream.qrpipe;

import java.util.BitSet;

/**
 * Used to mark the progress of decoding stream of QR codes.
 */
public class DecodeState {

	private BitSet data = null;
	private int capacity = 0;

	/**
	 * Initialize DecodeState with given capacity. After initialization, requests
	 * to change capacity are ignored. This allows external applications to query
	 * status of transmission and QR decoding, in the case that {@code Receive}
	 * is has not yet received and decoded its first QR code. It needs at least
	 * one QR code to know how many QR codes it should expect to receive.
	 */
	public void setInitialCapacity(int capacity) {
		if (this.data == null) {
			if (capacity < 0) {
				throw new IllegalArgumentException("DecodeState must have capacity > 0");
			}
			this.capacity = capacity;
			this.data = new BitSet(capacity);
		}
	}

	/**
	 * Return the state that a transmission is in. If no QR codes have been
	 * received and decoded, this returns the {@code Initial} state. When at
	 * least one QR code has been decoded, return {@code Intermediate} state.
	 * Finally, when all QR codes have been received and decoded, the state
	 * transitions to {@code Final}.
	 */
	public State getState () {
		if (data == null || data.isEmpty()) {
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
		if (data != null) {
			if (chunkId < 1 || chunkId > capacity) {
				throw new IndexOutOfBoundsException("Cannot mark bit, chunkId: " + chunkId +
				                                    ", is out of bounds");
			}
			data.set(chunkId - 1);
		}
	}

	//TODO: This was in master, but maybe not necessary?
	public void set(int position) {
		data.set(position);
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
