package org.ophion.jujube.internal;

import org.apache.hc.core5.http.HttpException;

public class RequestEntityLimitExceeded extends HttpException {
  public RequestEntityLimitExceeded(String message) {
    super(message);
  }
}
