package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;

@SuppressWarnings("unused")
public class HttpResponses {
  public static JujubeResponse ok() {
    return new JujubeResponse(HttpStatus.SC_OK);
  }

  public static JujubeResponse ok(String message) {
    return new JujubeResponse(message);
  }

  public static JujubeResponse noContent() {
    return new JujubeResponse(HttpStatus.SC_NO_CONTENT);
  }

  public static JujubeResponse badRequest(String message) {
    var r = new JujubeResponse(HttpStatus.SC_BAD_REQUEST);
    r.setContent(message);
    return r;
  }

  public static JujubeResponse notFound() {
    return new JujubeResponse(HttpStatus.SC_NOT_FOUND);
  }
}
