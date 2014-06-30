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

/**
 * Covers the range of exceptions which may occur when
 * receiving images and decoding the QR codes within them.
 */
public final class ReceiveException extends Exception {

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
