package org.ophion.jujube.config;

import org.ophion.jujube.middleware.Middleware;
import org.ophion.jujube.request.FormParameterExtractor;
import org.ophion.jujube.request.MultipartParameterExtractor;
import org.ophion.jujube.request.ParameterExtractor;
import org.ophion.jujube.request.PathParameterExtractor;
import org.ophion.jujube.routing.Route;
import org.ophion.jujube.routing.RouteGroup;
import org.ophion.jujube.routing.RouteHandler;
import org.ophion.jujube.routing.RoutePatterns;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

public class JujubeConfig {
  private final ServerConfig serverConfig;
  private final List<Route> routes;
  private final StaticAssetsConfig staticAssetsConfig;
  private ExecutorService executorService;
  private Charset defaultCharset;
  private List<ParameterExtractor> parameterExtractorRegistry;
  private List<Middleware> middlewareRegistry;

  public JujubeConfig() {
    this.routes = new ArrayList<>();
    this.middlewareRegistry = new ArrayList<>();
    this.executorService = ForkJoinPool.commonPool();
    this.serverConfig = new ServerConfig();
    this.staticAssetsConfig = new StaticAssetsConfig();
    this.defaultCharset = StandardCharsets.UTF_8;

    parameterExtractorRegistry = new ArrayList<>();
    parameterExtractorRegistry.add(new PathParameterExtractor());
    parameterExtractorRegistry.add(new MultipartParameterExtractor());
    parameterExtractorRegistry.add(new FormParameterExtractor());
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public void route(String railsLikePattern, RouteHandler handler) {
    route(RoutePatterns.railsRouteToRegex(railsLikePattern), handler);
  }

  public void route(Pattern regexPattern, RouteHandler handler) {
    route(new Route(regexPattern, handler));
  }

  public void route(Route route) {
    routes.add(route);
  }

  public void route(RouteGroup group) {
    routes.addAll(group.getRouteGroup());
  }

  public void addMiddleware(Middleware middleware) {
    this.middlewareRegistry.add(middleware);
  }

  public List<Middleware> getMiddlewareRegistry() {
    return middlewareRegistry;
  }

  public List<Route> routes() {
    return routes;
  }

  public List<ParameterExtractor> getParameterExtractorRegistry() {
    return parameterExtractorRegistry;
  }

  public Charset getDefaultCharset() {
    return defaultCharset;
  }

  public void setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  public StaticAssetsConfig getStaticAssetsConfig() {
    return staticAssetsConfig;
  }
}
