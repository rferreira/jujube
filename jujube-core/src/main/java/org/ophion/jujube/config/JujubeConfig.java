package org.ophion.jujube.config;

import org.ophion.jujube.route.RouteHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class JujubeConfig {
  private final ServerConfig serverConfig;
  private final Map<String, RouteHandler> routes;
  private ExecutorService executorService;
  
  public JujubeConfig() {
    this.routes = new LinkedHashMap<>();
    this.executorService = ForkJoinPool.commonPool();
    this.serverConfig = new ServerConfig();
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

  public void route(String uriPattern, RouteHandler handler) {
    this.routes.put(uriPattern, handler);
  }

  public Map<String, RouteHandler> routes() {
    return routes;
  }
}
