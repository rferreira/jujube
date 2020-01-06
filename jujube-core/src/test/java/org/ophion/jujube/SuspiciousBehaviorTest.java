package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.HttpResponse;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RandomInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class SuspiciousBehaviorTest extends IntegrationTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldLimitPostSize() throws IOException {
    var size = DataSize.megabytes(11);
    config.route("/post", ctx -> {
      try {
        long bytes = Files.size(Paths.get(ctx.getParameter("file", ParameterSource.FORM).orElseThrow()));
        Assertions.assertEquals(size.toBytes(), bytes);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

      var response = new HttpResponse("w00t");
      response.setCode(200);
      return response;
    });

    config.getServerConfig().setPostBodySizeLimit(DataSize.megabytes(10));

    server.start();

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RandomInputStream(size.toBytes()))
      .build();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(entity);

    client.execute(request, response -> {
      Assertions.assertEquals(413, response.getCode());
      return true;
    });
  }

  @Test
  void shouldShutdownIdleConnections() throws IOException, InterruptedException {
    config.route("/*", ctx -> {
      var response = new HttpResponse("w00t");
      response.setCode(200);
      return response;
    });

    config.getServerConfig().setIoReactorConfig(IOReactorConfig.custom()
      .setSoTimeout(500, TimeUnit.MILLISECONDS)
      .build());

    server.start();

    LOG.info("making socket connection");
    Socket socket = new Socket("localhost", config.getServerConfig().getListenPort());
    Assertions.assertTrue(socket.isConnected());
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    Assertions.assertFalse(socket.isConnected());
  }
}
