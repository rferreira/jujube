package org.ophion.jujube.http;

import org.ophion.jujube.response.JujubeHttpResponse;

public class HttpResponses {
  public static JujubeHttpResponse ok() {
    return new JujubeHttpResponse();
  }

  public static JujubeHttpResponse ok(String message) {
    return new JujubeHttpResponse(message);
  }
}
