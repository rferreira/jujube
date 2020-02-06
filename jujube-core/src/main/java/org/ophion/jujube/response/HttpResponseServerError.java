package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class HttpResponseServerError extends JujubeHttpResponse {
  public HttpResponseServerError() {
    super(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }
}
