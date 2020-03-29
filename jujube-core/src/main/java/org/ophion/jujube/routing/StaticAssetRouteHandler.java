package org.ophion.jujube.routing;

import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeResponse;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A route handler that loads static assets from the classpath.
 * <p>
 * Based off: https://github.com/dropwizard/dropwizard/blob/master/dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java
 */
public class StaticAssetRouteHandler implements RouteHandler, RouteGroup {
  private final String resourcePathPrefix;
  private final String indexFile;
  private final String uriPathPrefix;

  public StaticAssetRouteHandler(String uriPathPrefix, String resourcePathPrefix, String indexFile) {
    Objects.requireNonNull(indexFile);
    Objects.requireNonNull(uriPathPrefix);
    Objects.requireNonNull(resourcePathPrefix);

    Args.check(resourcePathPrefix.startsWith("/"), "resource path prefix must start with a \"/\"");
    Args.check(resourcePathPrefix.endsWith("/"), "resource path prefix must end with a \"/\"");
    Args.check(uriPathPrefix.startsWith("/"), "URI path prefix must start with a \"/\"");
    Args.check(uriPathPrefix.endsWith("/"), "URI path prefix must end with a \"/\"");

    this.resourcePathPrefix = resourcePathPrefix;
    this.indexFile = indexFile;
    this.uriPathPrefix = uriPathPrefix;
  }

  @Override
  public JujubeResponse handle(JujubeRequest req, HttpContext ctx) throws Exception {
    throw new IllegalStateException("Not Implemented");
  }

  public String getResourcePathPrefix() {
    return resourcePathPrefix;
  }

  public String getIndexFile() {
    return indexFile;
  }

  public String getUriPathPrefix() {
    return uriPathPrefix;
  }

  @Override
  public List<Route> getRouteGroup() {
    var pattern = String.format("^%s([^\\s]+)?$", Pattern.quote(uriPathPrefix));
    return List.of(
      new Route(Pattern.compile(pattern), this)
    );
  }
}
