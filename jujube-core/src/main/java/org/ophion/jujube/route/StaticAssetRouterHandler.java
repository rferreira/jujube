package org.ophion.jujube.route;

import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeResponse;

import java.util.Objects;

/**
 * A route handler that loads static assets from the classpath.
 * <p>
 * Based off: https://github.com/dropwizard/dropwizard/blob/master/dropwizard-assets/src/main/java/io/dropwizard/assets/AssetsBundle.java
 */
public class StaticAssetRouterHandler implements RouteHandler {
  private final String resourcePathPrefix;
  private final String indexFile;

  public StaticAssetRouterHandler() {
    this("/", "index.htm");
  }

  public StaticAssetRouterHandler(String resourcePathPrefix, String indexFile) {
    Objects.requireNonNull(indexFile);
    Objects.requireNonNull(resourcePathPrefix);

    if (!resourcePathPrefix.startsWith("/")) {
      throw new IllegalArgumentException("resource path prefix must start with a \"/\"");
    }

    this.resourcePathPrefix = resourcePathPrefix;
    this.indexFile = indexFile;
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
}
