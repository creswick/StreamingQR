package com.galois.qrstream.qrpipe;

import java.util.BitSet;

/**
 * Used to mark the progress of decoding stream of QR codes.
 */
public class DecodeState {
	
	private final BitSet data;
	private final int capacity;
		
	public DecodeState (int capacity) {
		this.capacity = capacity;
		this.data = new BitSet(capacity);
	}
	
	public State getState () {
		if (data.isEmpty()) {
			return State.Initial;
		} else if (data.cardinality() == this.capacity) {
			return State.Final;
		} else {
			return State.Intermediate;
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
}
