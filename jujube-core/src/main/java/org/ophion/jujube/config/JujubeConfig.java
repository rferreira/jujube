package org.ophion.jujube.config;

import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.response.JujubeHttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public class JujubeConfig {
  private final ServerConfig serverConfig = new ServerConfig();
  private ExecutorService executorService = ForkJoinPool.commonPool();
  private Map<String, Function<JujubeHttpContext, JujubeHttpResponse>> routes = new LinkedHashMap<>();

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public void route(String uriPattern, Function<JujubeHttpContext, JujubeHttpResponse> handler) {
    this.routes.put(uriPattern, handler);
  }

  public Map<String, Function<JujubeHttpContext, JujubeHttpResponse>> routes() {
    return routes;
  }

  public JujubeConfig() {
  }
}
