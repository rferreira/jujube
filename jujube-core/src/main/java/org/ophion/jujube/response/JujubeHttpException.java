package org.ophion.jujube.response;

public class JujubeHttpException extends RuntimeException {
  private final JujubeResponse response;

  public JujubeHttpException(JujubeResponse resp) {
    this.response = resp;
  }

  public JujubeHttpException(int code) {
    this.response = new JujubeResponse(code);
  }

  public JujubeHttpException(int code, String message) {
    this.response = new JujubeResponse(code);
    this.response.setContent(message);
  }

  public JujubeResponse toHttpResponse() {
    return this.response;
  }
}
