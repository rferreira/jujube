package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.ophion.jujube.JujubeHttpContext;
import org.ophion.jujube.response.JujubeHttpException;

import java.util.function.Supplier;

public class HttpConstraints {
  public static void onlyAllowMethod(Method method, JujubeHttpContext ctx) {
    onlyAllowMethod(method, ctx, () -> {
      throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
    });
  }

  public static void onlyAllowMethod(Method method, JujubeHttpContext ctx, Supplier<RuntimeException> exceptionSupplier) {
    if (method.isSame(ctx.getRequest().getMethod())) {
      return;
    }
    throw exceptionSupplier.get();
  }

  public static void onlyAllowMediaType(ContentType contentType, JujubeHttpContext ctx) {
    ctx.getEntityContentType().ifPresent(ct -> {
      if (!ct.isSameMimeType(contentType)) {
        throw new JujubeHttpException(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
      }
    });
  }
}
