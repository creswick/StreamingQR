package com.galois.qrstream.qrpipe;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public enum CorrectionLevel {
  /** L = ~7% error correction */
  L(ErrorCorrectionLevel.L),
  /** M = ~15% error correction */
  M(ErrorCorrectionLevel.M),
  /** Q = ~25% error correction */
  Q(ErrorCorrectionLevel.Q),
  /** H = ~30% error correction */
  H(ErrorCorrectionLevel.H);

  private final ErrorCorrectionLevel ecLevel;

  private CorrectionLevel(ErrorCorrectionLevel ecLevel) {
    this.ecLevel = ecLevel;
  }

  protected ErrorCorrectionLevel toZXingECLevel() {
    return ecLevel;
  }
}
