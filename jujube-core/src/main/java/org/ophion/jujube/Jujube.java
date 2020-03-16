package org.ophion.jujube;

import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apache.hc.core5.util.TimeValue;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.JujubeAssetsServerExchangeHandler;
import org.ophion.jujube.internal.JujubeServerExchangeHandler;
import org.ophion.jujube.internal.util.Durations;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.route.StaticAssetRouteHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Just enough logic to turn Apache Http Core into something suited for micro services.
 *
 * Main server logic, starts and listens for HTTP requests.
 */
public class Jujube {
  private static final Logger LOG = Loggers.build();
  private final JujubeConfig config;
  private HttpAsyncServer instance;

  public Jujube(JujubeConfig config) {
    this.config = config;
  }

  /**
   * Starts a new server and then blocks the caller thread forever.
   * @throws InterruptedException if we're interrupted.
   */
  public void startAndWait() throws InterruptedException {
    this.start();
    instance.awaitShutdown(TimeValue.MAX_VALUE);
  }

  /**
   * Starts a new server.
   */
  public void start() {
    Instant startInstant = Instant.now();

    try {
      final var banner = Files.readString(Paths.get(Objects.requireNonNull(Jujube.class.getClassLoader().getResource("banner.txt")).toURI()));
      System.out.println(banner);

      final var bootstrap = H2ServerBootstrap.bootstrap()
        .setH2Config(config.getServerConfig().getH2Config())
        .setHttp1Config(config.getServerConfig().getHttp1Config())
        .setTlsStrategy(config.getServerConfig().getTlsStrategy())
        .setIOReactorConfig(config.getServerConfig().getIoReactorConfig())
        .setHandshakeTimeout(config.getServerConfig().getHandshakeTimeout())
        .setExceptionCallback(config.getServerConfig().getExceptionCallback())
        .setLookupRegistry(config.getServerConfig().getLookupRegistry())
        .setCanonicalHostName(config.getServerConfig().getCanonicalHostName())
        .setVersionPolicy(config.getServerConfig().getVersionPolicy());

      if (config.getServerConfig().getH2StreamListener() != null) {
        bootstrap.setStreamListener(config.getServerConfig().getH2StreamListener());
      }

      if (config.getServerConfig().getHttp1StreamListener() != null) {
        bootstrap.setStreamListener(config.getServerConfig().getHttp1StreamListener());
      }

      config.routes()
        .forEach((path, handler) -> {
          LOG.info("adding route: {} -> {}", path, handler);
          if (handler instanceof StaticAssetRouteHandler) {
            var staticHandler = (StaticAssetRouteHandler) handler;
            bootstrap.register(path, () -> new JujubeAssetsServerExchangeHandler(config, path, staticHandler.getResourcePathPrefix(), staticHandler.getIndexFile()));
          } else {
            bootstrap.register(path, () -> new JujubeServerExchangeHandler(config, handler));
          }
        });

      this.instance = bootstrap.create();

      Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

      this.instance.start();

      final Future<ListenerEndpoint> future = instance.listen(new InetSocketAddress(config.getServerConfig().getListenPort()));
      final ListenerEndpoint listenerEndpoint = future.get();

      var isTlsEnabled = config.getServerConfig().getTlsStrategy() != null;
      System.out.println(String.format("> HTTP server started (on %s) in %s with %d known route(s) and TLS %s, enjoy \uD83C\uDF89",
        listenerEndpoint.toString(),
        Durations.humanize(Duration.between(startInstant, Instant.now())), config.routes().size(),
        isTlsEnabled ? "ON" : "OFF")
      );

    } catch (InterruptedException | ExecutionException | URISyntaxException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Stops the running server awaiting in-flight requests to finish.
   */
  public void stop() {
    var shutdownDelay = config.getServerConfig().getShutDownDelay();
    System.out.println(String.format("> HTTP server is shutting down, awaiting %s for in-flight requests...", Durations.humanize(shutdownDelay)));
    try {
      if (shutdownDelay.isZero()) {
        instance.close(CloseMode.IMMEDIATE);
        instance.initiateShutdown();
        instance.awaitShutdown(TimeValue.ZERO_MILLISECONDS);
      } else {
        instance.close(CloseMode.GRACEFUL);
        instance.initiateShutdown();
        instance.awaitShutdown(TimeValue.ofMilliseconds(shutdownDelay.toMillis()));
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
    LOG.warn("server {} shutdown", instance);
  }
}
