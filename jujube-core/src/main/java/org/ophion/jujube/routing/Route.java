package org.ophion.jujube.routing;

import java.util.regex.Pattern;

public class Route {
  private final Pattern pattern;
  private final RouteHandler handler;

  public Route(Pattern pattern, RouteHandler handler) {
    this.pattern = pattern;
    this.handler = handler;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public RouteHandler getHandler() {
    return handler;
  }

  @Override
  public String toString() {
    return String.format("Regex:%s routes to: %s", pattern, handler.getClass().getCanonicalName());
  }
}
