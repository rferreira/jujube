package org.ophion.jujube.example;

import org.ophion.jujube.Jujube;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.example.resources.ChecksumResource;
import org.ophion.jujube.example.resources.EchoResource;
import org.ophion.jujube.route.StaticAssetRouteHandler;

public class JujubeHelloWorld {
  public static void main(String[] args) throws InterruptedException {
    var config = new JujubeConfig();
    config.getServerConfig().disableTls();

    var echoResource = new EchoResource();
    config.route("/echo/hello*", echoResource::hello);
    config.route("/echo/404", echoResource::notFound);

    ChecksumResource checksumResource = new ChecksumResource();
    config.route("/checksum/*", checksumResource::post);
    // catch all:
    config.route("/*", new StaticAssetRouteHandler("/assets/", "index.html"));

    Jujube server = new Jujube(config);
    server.startAndWait();
  }
}
