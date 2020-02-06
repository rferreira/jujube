package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class HttpResponseNotFound extends JujubeHttpResponse {
  public HttpResponseNotFound() {
    super(HttpStatus.SC_NOT_FOUND);
  }
}
