package com.galois.qrstream.qrpipe;

import com.galois.qrstream.qrpipe.DecodeState;

public interface IProgress {
  void changeState(DecodeState state);
}
