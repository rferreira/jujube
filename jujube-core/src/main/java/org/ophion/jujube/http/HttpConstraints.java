package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHeader;
import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeHttpResponse;

import java.util.function.Supplier;

public class HttpConstraints {
  public static void onlyAllowMethod(Method method, JujubeHttpContext ctx) {
    onlyAllowMethod(method, ctx, () -> {
      var resp = new JujubeHttpResponse(HttpStatus.SC_METHOD_NOT_ALLOWED);
      resp.setHeader(new BasicHeader(HttpHeaders.ALLOW, method.toString()));
      throw new JujubeHttpException(resp);
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
