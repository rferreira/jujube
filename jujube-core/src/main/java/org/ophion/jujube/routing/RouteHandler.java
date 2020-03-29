package org.ophion.jujube.routing;

import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeResponse;

@FunctionalInterface
public interface RouteHandler {
  JujubeResponse handle(JujubeRequest req, HttpContext ctx) throws Exception;
}
