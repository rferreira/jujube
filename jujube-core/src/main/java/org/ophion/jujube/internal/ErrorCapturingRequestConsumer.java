package org.ophion.jujube.internal;

import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;

import java.util.concurrent.atomic.AtomicReference;

public class ErrorCapturingRequestConsumer<T> extends BasicRequestConsumer<T> {
  AtomicReference<Exception> exceptionAtomicReference;

  public ErrorCapturingRequestConsumer(AtomicReference<Exception> exceptionRef, AsyncEntityConsumer<T> dataConsumer) {
    super(dataConsumer);
    this.exceptionAtomicReference = exceptionRef;
  }

  @Override
  public void failed(Exception cause) {
    super.failed(cause);
    this.exceptionAtomicReference.set(cause);
  }
}
