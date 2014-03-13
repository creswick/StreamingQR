package com.galois.qrstream.qrpipe;

/**
 * Class covers the range of exceptions which may occur when
 * receiving images and decoding the QR codes within them.
 */
public class ReceiveException extends Exception {

  /**
   * Compiler-generated ID. If file undergoes structural
   * changes this field will need to be updated.
   */
  private static final long serialVersionUID = 5345098537585037274L;

  public ReceiveException() {
  }

  public ReceiveException(String message) {
    super(message);
  }

  public ReceiveException(Throwable cause) {
    super(cause);
  }

  public ReceiveException(String message, Throwable cause) {
    super(cause);
  }
}
