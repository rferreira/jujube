package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class ResponseRequestTooLarge extends JujubeResponse {
  public ResponseRequestTooLarge() {
    super(HttpStatus.SC_REQUEST_TOO_LONG);
  }
}
