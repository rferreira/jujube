package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.HttpResponses;
import org.ophion.jujube.response.JujubeHttpException;

import java.util.Set;

/**
 * HTTP Preconditions.
 */
public class HttpConstraints {
  public static void onlyAllowMethod(Set<Method> methods, JujubeRequest req) {
    methods.stream()
      .filter(method -> method.isSame(req.getMethod()))
      .findAny()
      .orElseThrow(() -> {
        var resp = HttpResponses.methodNotAllowed(methods);
        throw new JujubeHttpException(resp);
      });
  }

  public static void onlyAllowMethod(Method method, JujubeRequest req) {
    onlyAllowMethod(Set.of(method), req);
  }

  public static void onlyAllowMediaType(ContentType allowedRange, JujubeRequest req) {
    req.getHttpEntity().ifPresent(entity -> {
      ContentType entityContentType = ContentType.parseLenient(entity.getContentType());
      if (!ContentTypes.isInRange(entityContentType, allowedRange)) {
        throw new JujubeHttpException(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
      }
    });
  }
}
