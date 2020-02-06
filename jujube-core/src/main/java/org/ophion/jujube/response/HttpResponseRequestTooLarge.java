package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class HttpResponseRequestTooLarge extends JujubeHttpResponse {
  public HttpResponseRequestTooLarge() {
    super(HttpStatus.SC_REQUEST_TOO_LONG);
  }
}
