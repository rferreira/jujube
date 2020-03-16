package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.response.JujubeResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class JujubeTlsTest extends IntegrationTest {
  @Test
  void shouldAllowUsersToDisable() throws IOException {
    AtomicInteger counter = new AtomicInteger();
    config.route("/hello", (req, ctx) -> {
      var response = new JujubeResponse("w00t");
      response.setCode(207);
      counter.incrementAndGet();
      return response;
    });

    config.getServerConfig().setListenPort(8443);
    config.getServerConfig().disableTls();
    server.start();

    client.execute(new HttpGet("http://localhost:8443/hello"), response -> {
      Assertions.assertEquals(207, response.getCode());
      return true;
    });

    Assertions.assertEquals(1, counter.get());
  }

  @Test
  void shouldEnableTlsWithSelfSignedCert() throws IOException {

    AtomicInteger counter = new AtomicInteger();
    config.route("/hello", (req, ctx) -> {
      var response = new JujubeResponse("w00t");
      response.setCode(207);
      counter.incrementAndGet();
      return response;
    });

    config.getServerConfig().setListenPort(8080);
    server.start();

    client.execute(new HttpGet("https://localhost:8080/hello"), response -> {
      Assertions.assertEquals(207, response.getCode());
      return true;
    });

    Assertions.assertEquals(1, counter.get());
  }
}
