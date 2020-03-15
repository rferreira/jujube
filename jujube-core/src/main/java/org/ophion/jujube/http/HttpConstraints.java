package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHeader;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeResponse;

import java.util.function.Supplier;

public class HttpConstraints {
  public static void onlyAllowMethod(Method method, JujubeRequest req) {
    onlyAllowMethod(method, req, () -> {
      var resp = new JujubeResponse(HttpStatus.SC_METHOD_NOT_ALLOWED);
      resp.setHeader(new BasicHeader(HttpHeaders.ALLOW, method.toString()));
      throw new JujubeHttpException(resp);
    });
  }

  public static void onlyAllowMethod(Method method, JujubeRequest req, Supplier<RuntimeException> exceptionSupplier) {
    if (method.isSame(req.getMethod())) {
      return;
    }
    throw exceptionSupplier.get();
  }

  public static void onlyAllowMediaType(ContentType contentType, JujubeRequest req) {
    req.getHttpEntity().ifPresent(entity -> {
      if (!ContentType.parseLenient(entity.getContentType()).isSameMimeType(contentType)) {
        throw new JujubeHttpException(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
      }
    });
  }
}
