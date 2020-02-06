package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpResponse;

public class JujubeHttpException extends RuntimeException {
  private final JujubeHttpResponse response;

  public JujubeHttpException(JujubeHttpResponse resp) {
    this.response = resp;
  }

  public JujubeHttpException(int code) {
    this.response = new JujubeHttpResponse(code);
  }

  public JujubeHttpException(int code, String message) {
    this.response = new JujubeHttpResponse(code);
    this.response.setContent(message);
  }

  public HttpResponse toHttpResponse() {
    return this.response;
  }
}
