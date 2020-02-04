package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.nio.support.ImmediateResponseExchangeHandler;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.JujubeServerExchangeHandler;
import org.ophion.jujube.response.HttpResponse;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RandomInputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Disabled
public class Triage {
  @Test
  void testFoo() throws ExecutionException, InterruptedException, IOException {
    int port = 9999;
    var config = new JujubeConfig();
    config.getServerConfig().setRequestEntityLimit(DataSize.megabytes(2));

    final HttpAsyncServer server = H2ServerBootstrap.bootstrap()
      .register("*", () -> new ImmediateResponseExchangeHandler(413, "hello world"))
      .setExceptionCallback(Throwable::printStackTrace)
      .create();

    server.start();
    final Future<ListenerEndpoint> future = server.listen(new InetSocketAddress(port));
    final ListenerEndpoint listenerEndpoint = future.get();

    var size = DataSize.gigabytes(1);

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RandomInputStream(size.toBytes()))
      .build();

    var request = new HttpPost("http://localhost:" + port + "/post");
    request.setEntity(entity);

    var client = HttpClients.createDefault();
    client.execute(request, response -> {
      Assertions.assertEquals(413, response.getCode());
      return true;
    });
  }

  @Test
  void testBasic() throws ExecutionException, InterruptedException, IOException {
    int port = 9999;
    var config = new JujubeConfig();
    config.getServerConfig().setRequestEntityLimit(DataSize.megabytes(2));

    final HttpAsyncServer server = H2ServerBootstrap.bootstrap()
      .register("*", () -> new JujubeServerExchangeHandler(config, ctx -> {
        return new HttpResponse();
      }))
      .setExceptionCallback(Throwable::printStackTrace)
      .create();

    server.start();
    final Future<ListenerEndpoint> future = server.listen(new InetSocketAddress(port));
    final ListenerEndpoint listenerEndpoint = future.get();

    var size = DataSize.terabytes(1);

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RandomInputStream(size.toBytes()))
      .build();

    var request = new HttpPost("http://localhost:" + port + "/post");
    request.setEntity(entity);

    var client = HttpClients.createDefault();
    client.execute(request, response -> {
      Assertions.assertEquals(413, response.getCode());
      return true;
    });
  }
}
