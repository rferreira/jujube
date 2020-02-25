package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

public class HttpResponses {
  public static JujubeHttpResponse ok() {
    return new JujubeHttpResponse();
  }

  public static JujubeHttpResponse ok(String message) {
    return new JujubeHttpResponse(message);
  }

  public static JujubeHttpResponse badRequest(String message) {
    var r = new JujubeHttpResponse(HttpStatus.SC_BAD_REQUEST);
    r.setContent(message);
    return r;
  }
}
