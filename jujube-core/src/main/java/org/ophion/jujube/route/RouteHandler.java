package org.ophion.jujube.route;

import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.response.JujubeHttpResponse;

@FunctionalInterface
public interface RouteHandler {
  JujubeHttpResponse handle(JujubeHttpContext ctx) throws Exception;
}
