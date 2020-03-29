package org.ophion.jujube.middleware;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs using an approximation of the <a href="https://httpd.apache.org/docs/current/logs.html#common">Common Log Format</a>
 */
public class AccessLogMiddleware extends Middleware {
  public static final String REQUEST_START_ATTRIBUTE = "jujube.request.start";
  private static final DateTimeFormatter CLF_FORMAT = DateTimeFormatter.ofPattern("dd/LLL/yyyy:HH:mm:ss Z");

  @Override
  public void onBeforeContentConsumption(HttpRequest request, HttpContext context) {
    context.setAttribute(REQUEST_START_ATTRIBUTE, LocalDateTime.now().atZone(ZoneId.systemDefault()));
  }

  @Override
  public void onAfterHandler(JujubeRequest request, JujubeResponse response, HttpContext context) {
    var ctx = (HttpCoreContext) context;
    ZonedDateTime requestStart = ctx.getAttribute(REQUEST_START_ATTRIBUTE, ZonedDateTime.class);
    var endpointDetails = ((HttpCoreContext) context).getEndpointDetails();
    var line = String.format("%s - - [%s] \"%s %s %s\" %d %d",
      endpointDetails.getRemoteAddress(),
      CLF_FORMAT.format(requestStart),
      request.getMethod(),
      request.getPath(),
      request.getVersion().toString(),
      response.getCode(),
      endpointDetails.getSentBytesCount()
    );

    System.out.println(line);
  }
}
