package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class HttpResponseInternalEngineError extends HttpResponse {
  public HttpResponseInternalEngineError() {
    super(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }
}
