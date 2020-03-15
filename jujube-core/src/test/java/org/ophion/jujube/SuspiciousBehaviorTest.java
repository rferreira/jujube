package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.FileParameter;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RandomInputStream;
import org.ophion.jujube.util.RepeatingInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SuspiciousBehaviorTest extends IntegrationTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldLimitPostSize() throws IOException {
    var size = DataSize.megabytes(2);
    config.route("/post", (req, ctx) -> {
      try {
        var file = (FileParameter) req.getParameter("file", ParameterSource.FORM).orElseThrow();
        long bytes = Files.size(file.asPath());
        Assertions.assertEquals(size.toBytes(), bytes);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return new JujubeResponse("w00t");
    });

    config.getServerConfig().setRequestEntityLimit(DataSize.megabytes(1));
    server.start();

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RepeatingInputStream(size.toBytes()))
      .build();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(entity);

    client.execute(request, response -> {
      Assertions.assertEquals(413, response.getCode());
      return true;
    });
  }

  @Test
  void shouldLimitPostSizeByContentLength() throws IOException {
    config.route("/post", (req, ctx) -> {
      Assertions.fail("request should not be processed");
      return null;
    });

    config.getServerConfig().setRequestEntityLimit(DataSize.bytes(0));
    server.start();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(HttpEntities.create("hello"));

    client.execute(request, response -> {
      Assertions.assertEquals(413, response.getCode());
      return true;
    });
  }


  @Test
  void shouldShutdownIdleConnections() throws IOException, InterruptedException {
    config.route("/*", (req, ctx) -> {
      return new JujubeResponse("w00t");
    });

    // we need to set both the timeout, and the select internal since they relate to one another
    config.getServerConfig().setIoReactorConfig(IOReactorConfig.custom()
      .setSoTimeout(500, TimeUnit.MILLISECONDS)
      .setSelectInterval(TimeValue.of(100, TimeUnit.MILLISECONDS))
      .build());

    server.start();

    LOG.info("making socket connection");
    Socket socket = new Socket("localhost", config.getServerConfig().getListenPort());
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    Assertions.assertThrows(SocketException.class, () -> {
      socket.getOutputStream().write(42);
    });
  }

  @Test
  void shouldPreventFormMasqueradingAsFile() throws IOException {
    final String stringThatLooksLikePath = "/etc/passwd";
    config.route("/post", (req, ctx) -> {
      try {
        var files = req.getParameters().stream()
          .filter(p -> p.name().equals("file"))
          .collect(Collectors.toList());
        Assertions.assertEquals(2, files.size());

        files.forEach(f -> {
          Assertions.assertFalse(f.isText());
        });

        var f0 = (FileParameter) files.get(0);
        var f1 = (FileParameter) files.get(1);

        var contents = Files.readString(f0.asPath());
        Assertions.assertEquals(stringThatLooksLikePath, contents);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }

      return new JujubeResponse("w00t");
    });

    server.start();

    // this is to ensure that we do not treat multiple params with the same name as a single param,
    // in particular multipart vs form encoded:
    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RandomInputStream(42))
      .addTextBody("file", stringThatLooksLikePath, ContentType.APPLICATION_OCTET_STREAM)
      .build();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(entity);

    client.execute(request, response -> {
      Assertions.assertEquals(200, response.getCode());
      return true;
    });
  }
}
