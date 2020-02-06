package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.response.JujubeHttpResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class JujubeTest extends IntegrationTest {
  @Test
  void shouldEmbedServer() throws IOException {
    server.start();
    client.execute(new HttpGet(endpoint), response -> {
      Assertions.assertEquals(404, response.getCode());
      return true;
    });
  }

  @Test
  void shouldRouteRequests() throws IOException {
    AtomicInteger counter = new AtomicInteger();
    config.route("/hello", (ctx) -> {
      var response = new JujubeHttpResponse("w00t");
      response.setCode(207);
      counter.incrementAndGet();
      return response;
    });

    server.start();

    client.execute(new HttpGet(endpoint.resolve("/hello")), response -> {
      Assertions.assertEquals(207, response.getCode());
      return true;
    });

    Assertions.assertEquals(1, counter.get());
  }

  @Test
  void shouldHandleRawRequests() throws IOException {
    final var contents = "{\n" +
      "  \"firstName\": \"John\",\n" +
      "  \"lastName\": \"Smith\",\n" +
      "  \"isAlive\": true,\n" +
      "  \"age\": 27,\n" +
      "  \"address\": {\n" +
      "    \"streetAddress\": \"21 2nd Street\",\n" +
      "    \"city\": \"New York\",\n" +
      "    \"state\": \"NY\",\n" +
      "    \"postalCode\": \"10021-3100\"\n" +
      "  },\n" +
      "  \"phoneNumbers\": [\n" +
      "    {\n" +
      "      \"type\": \"home\",\n" +
      "      \"number\": \"212 555-1234\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"type\": \"office\",\n" +
      "      \"number\": \"646 555-4567\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"type\": \"mobile\",\n" +
      "      \"number\": \"123 456-7890\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"children\": [],\n" +
      "  \"spouse\": null\n" +
      "}";

    AtomicInteger counter = new AtomicInteger();
    config.route("/hello", (ctx) -> {
      var response = new JujubeHttpResponse();
      try {
        Assertions.assertEquals(contents, EntityUtils.toString(ctx.getEntity().orElseThrow()));
      } catch (IOException | ParseException e) {
        Assertions.fail(e);
      }
      counter.incrementAndGet();
      return response;
    });

    server.start();

    var req = new HttpPost(endpoint.resolve("/hello"));
    req.setEntity(EntityBuilder.create().setText(contents).build());
    client.execute(req, response -> {
      Assertions.assertEquals(HttpStatus.SC_NO_CONTENT, response.getCode());
      return true;
    });
    Assertions.assertEquals(1, counter.get());
  }
}
