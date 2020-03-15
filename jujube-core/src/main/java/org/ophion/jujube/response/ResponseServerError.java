package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class ResponseServerError extends JujubeResponse {
  public ResponseServerError() {
    super(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }
}
