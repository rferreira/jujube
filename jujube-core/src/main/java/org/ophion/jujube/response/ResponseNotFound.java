package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class ResponseNotFound extends JujubeResponse {
  public ResponseNotFound() {
    super(HttpStatus.SC_NOT_FOUND);
  }
}
