package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;

import java.util.Set;
import java.util.stream.Collectors;

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

  public static JujubeResponse badRequest() {
    return new JujubeResponse(HttpStatus.SC_BAD_REQUEST);
  }

  public static JujubeResponse badRequest(String message) {
    var r = new JujubeResponse(HttpStatus.SC_BAD_REQUEST);
    r.setContent(message);
    return r;
  }

  public static JujubeResponse notFound() {
    return new JujubeResponse(HttpStatus.SC_NOT_FOUND);
  }

  /**
   * Creates a new Method Not Allowed response with the required allow method header.
   *
   * @param allowedMethods the list of allowed HTTP methods.
   * @return a response!
   */
  public static JujubeResponse methodNotAllowed(Set<Method> allowedMethods) {
    var response = new JujubeResponse(HttpStatus.SC_METHOD_NOT_ALLOWED);
    response.setHeader(HttpHeaders.ALLOW, allowedMethods.stream().map(Enum::toString).collect(Collectors.joining(",")));
    return response;
  }
}
