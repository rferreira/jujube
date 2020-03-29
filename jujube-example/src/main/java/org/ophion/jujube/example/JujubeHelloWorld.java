package org.ophion.jujube.example;

import org.ophion.jujube.Jujube;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.example.resources.ChecksumResource;
import org.ophion.jujube.example.resources.EchoResource;
import org.ophion.jujube.middleware.AccessLogMiddleware;
import org.ophion.jujube.routing.RoutePatterns;
import org.ophion.jujube.routing.StaticAssetRouteHandler;

public class JujubeHelloWorld {
  public static void main(String[] args) throws InterruptedException {
    var config = new JujubeConfig();
//    config.getServerConfig().disableTls();
    config.addMiddleware(new AccessLogMiddleware());

    var echoResource = new EchoResource();
    config.route("/echo/hello", echoResource::hello);
    config.route(RoutePatterns.railsRouteToRegex("/echo/:code"), echoResource::notFound);

    ChecksumResource checksumResource = new ChecksumResource();
    config.route("/checksum", checksumResource::post);

    // catch all:
    config.route(new StaticAssetRouteHandler("/", "/assets/", "index.html"));

    Jujube server = new Jujube(config);
    server.startAndWait();
  }
}
