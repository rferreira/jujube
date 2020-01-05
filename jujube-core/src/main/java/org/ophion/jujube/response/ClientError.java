package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class ClientError extends JujubeHttpException {
  public ClientError(String message) {
    super(HttpStatus.SC_CLIENT_ERROR, message);
  }
}
