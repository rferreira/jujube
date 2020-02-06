package org.ophion.jujube.example;

import org.ophion.jujube.Jujube;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.example.resources.ChecksumResource;
import org.ophion.jujube.example.resources.EchoResource;
import org.ophion.jujube.response.JujubeHttpResponse;

public class JujubeHelloWorld {
  public static void main(String[] args) throws InterruptedException {
    var config = new JujubeConfig();

    var echoResource = new EchoResource();
    config.route("/echo/hello*", echoResource::hello);
    config.route("/echo/404", echoResource::notFound);

    ChecksumResource checksumResource = new ChecksumResource();
    config.route("/checksum/*", checksumResource::post);
    // catch all:
    config.route("/*", ctx -> {
      return new JujubeHttpResponse("Hello world from Jujube!");
    });

    Jujube server = new Jujube(config);
    server.startAndWait();
  }
}
