package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.context.FileParameter;
import org.ophion.jujube.context.ParameterSource;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.HttpResponse;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RepeatingInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

class MultiPartPostTest extends IntegrationTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldHandleMultiPartFormPosts() throws IOException {
    config.route("/post", (ctx) -> {
      LOG.info("here");
      Assertions.assertEquals("value1", ctx.getParameter("text1", ParameterSource.FORM).orElseThrow().value());
      try {
        var param = (FileParameter) (ctx.getParameter("file", ParameterSource.FORM).orElseThrow());
        Assertions.assertArrayEquals("00000".getBytes(), Files.readAllBytes(param.asPath()));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

      var response = new HttpResponse("w00t");
      response.setCode(200);
      return response;
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
    config.route("/post", ctx -> {
      try {
        var file = (FileParameter) ctx.getParameter("file", ParameterSource.FORM).orElseThrow();
        long bytes = Files.size(file.asPath());
        Assertions.assertEquals(size.toBytes(), bytes);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

      var response = new HttpResponse("w00t");
      response.setCode(200);
      return response;
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
  }
}
