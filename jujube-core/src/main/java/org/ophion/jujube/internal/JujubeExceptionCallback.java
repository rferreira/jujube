package org.ophion.jujube.internal;

import org.apache.hc.core5.function.Callback;

public class JujubeExceptionCallback implements Callback<Exception> {
  @Override
  public void execute(Exception ex) {
    ex.printStackTrace(System.err);
  }
}
