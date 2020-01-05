package org.ophion.jujube;

import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.response.HttpResponse;

public class JujubeHelloWorld {
  public static void main(String[] args) throws InterruptedException {
    var config = new JujubeConfig();
    config.route("/*", ctx -> {
      return new HttpResponse("Hello world");
    });
    Jujube server = new Jujube(config);
    server.startAndWait();
  }
}
