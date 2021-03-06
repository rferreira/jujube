package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.FileParameter;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RepeatingInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

class MultiPartPostTest extends IntegrationTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldHandleMultiPartFormPosts() throws IOException {
    config.route("/post", (req, ctx) -> {
      LOG.info("here");
      Assertions.assertEquals("value1", req.getParameter("text1", ParameterSource.FORM).orElseThrow().asText());
      try {
        var param = (FileParameter) (req.getParameter("file", ParameterSource.FORM).orElseThrow());
        Assertions.assertArrayEquals("00000".getBytes(), Files.readAllBytes(param.asPath()));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

      return new JujubeResponse("w00t");
    });

    server.start();

    HttpEntity entity = MultipartEntityBuilder.create()
      .addBinaryBody("file", "00000".getBytes())
      .addTextBody("text1", "value1")
      .build();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(entity);

    client.execute(request, response -> {
      Assertions.assertEquals(200, response.getCode());
      return true;
    });
  }

  @Test
  void shouldStreamLargeFiles() throws IOException {
    var size = DataSize.megabytes(10);
    AtomicInteger counter = new AtomicInteger();
    config.route("/post", (req, ctx) -> {
      try {
        var file = (FileParameter) req.getParameter("file", ParameterSource.FORM).orElseThrow();
        long bytes = Files.size(file.asPath());
        Assertions.assertEquals(size.toBytes(), bytes);
        counter.incrementAndGet();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

      return new JujubeResponse("w00t");
    });

    server.start();

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RepeatingInputStream(size.toBytes()))
      .addTextBody("hello", "world")
      .build();

    var request = new HttpPost(endpoint.resolve("/post"));
    request.setEntity(entity);

    client.execute(request, response -> {
      Assertions.assertEquals(200, response.getCode());
      return true;
    });

    Assertions.assertEquals(1, counter.get());
  }
}
