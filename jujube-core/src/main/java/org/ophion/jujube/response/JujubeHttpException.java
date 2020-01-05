package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpResponse;

public class JujubeHttpException extends RuntimeException {
  private final int code;
  private String message;

  public JujubeHttpException(int code) {
    this.code = code;
  }

  public JujubeHttpException(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public HttpResponse toHttpResponse() {
    var resp = new org.ophion.jujube.response.HttpResponse(this.code);
    if (this.message != null) {
      resp.setContent(message);
    }
    return resp;
  }
}
