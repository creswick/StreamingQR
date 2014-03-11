package com.galois.qrstream.qrpipe;

/**
 * Class covers the range of exceptions which may occur when
 * encoding and transmitting QR codes.
 * 
 */
public class TransmitException extends Exception {

  /**
   * Compiler-generated ID. If file undergoes structural
   * changes this field will need to be updated.
   */
  private static final long serialVersionUID = 6837223855749192876L;

  public TransmitException() {
  }

  public TransmitException(String message) {
    super(message);
  }

  public TransmitException(Throwable cause) {
    super(cause);
  }

  public TransmitException(String message, Throwable cause) {
    super(cause);
  }
}
