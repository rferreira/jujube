package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.middleware.Middleware;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class JujubeMiddlewareTest extends IntegrationTest {

  public static final String DEFAULT_PATH = "/hello";

  @Test
  void shouldNotProcessContentIfEarlyTerminated() throws IOException {
    AtomicInteger counter = new AtomicInteger();

    config.route(DEFAULT_PATH, (req, ctx) -> {
      counter.incrementAndGet();
      return new JujubeResponse(207, "w00t");
    });

    config.addMiddleware(new Middleware() {
      @Override
      public void onBeforeContentConsumption(HttpRequest req, HttpContext ctx) {
        if (req.getFirstHeader(HttpHeaders.AUTHORIZATION) == null) {
          throw new JujubeHttpException(401);
        }
      }
    });
    server.start();


    var req = new HttpPost(endpoint.resolve(DEFAULT_PATH));
    req.setEntity(MultipartEntityBuilder.create()
      .addTextBody("name", "theodore")
      .addTextBody("breed", "st bernard")
      .build()
    );

    client.execute(req, response -> {
      Assertions.assertEquals(401, response.getCode());
      return true;
    });

    Assertions.assertEquals(0, counter.get());
  }

  @Test
  void shouldAllowForCustomMiddleware() throws IOException {
    AtomicInteger counter = new AtomicInteger();

    config.route(DEFAULT_PATH, (req, ctx) -> {
      return new JujubeResponse(207, "w00t");
    });

    config.addMiddleware(new Middleware() {
      @Override
      public void onAfterHandler(JujubeRequest req, JujubeResponse res, HttpContext ctx) {
        counter.incrementAndGet();
      }
    });

    server.start();


    client.execute(new HttpGet(endpoint.resolve(DEFAULT_PATH)), response -> {
      Assertions.assertEquals(207, response.getCode());
      return true;
    });
    Assertions.assertEquals(1, counter.get());
  }
}
