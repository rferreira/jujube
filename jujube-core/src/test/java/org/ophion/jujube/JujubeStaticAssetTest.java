package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.http.ContentTypes;
import org.ophion.jujube.route.StaticAssetRouterHandler;

import java.io.IOException;

public class JujubeStaticAssetTest extends IntegrationTest {
  @Test
  void shouldHandleStaticAssetsFromClasspath() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var contents = client.execute(new HttpGet(endpoint.resolve("/static/hello.txt")), response -> {
      Assertions.assertEquals(200, response.getCode());
      Assertions.assertEquals(ContentType.TEXT_PLAIN.withCharset(config.getDefaultCharset()).toString(), response.getEntity().getContentType());
      return EntityUtils.toString(response.getEntity());
    });
    Assertions.assertEquals("hello world", contents.trim());
  }

  @Test
  void shouldHandleStaticFromSubDirectories() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var contents = client.execute(new HttpGet(endpoint.resolve("/static/dir1/foo.js")), response -> {
      Assertions.assertEquals(200, response.getCode());
      Assertions.assertEquals(ContentTypes.APPLICATION_JAVASCRIPT.toString(), response.getEntity().getContentType());
      return EntityUtils.toString(response.getEntity());
    });
    Assertions.assertEquals("console.log(\"hello world\");", contents.trim());
  }

  @Test
  void shouldNotAllowRelativePath() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var code = client.execute(new HttpGet(endpoint.resolve("/static/../assets-not-reachable.xml")), HttpResponse::getCode);
    Assertions.assertEquals(404, code);
  }

  @Test
  void should415OnInvalidMethods() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var code = client.execute(new HttpPost(endpoint.resolve("/static/hello.txt")), HttpResponse::getCode);
    Assertions.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, code);
  }

  @Test
  void shouldLoadIndexFile() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var contents = client.execute(new HttpGet(endpoint.resolve("/static/")), response -> {
      Assertions.assertEquals(200, response.getCode());
      Assertions.assertEquals(ContentType.TEXT_HTML.withCharset(config.getDefaultCharset()).toString(), response.getEntity().getContentType());
      return EntityUtils.toString(response.getEntity());
    });
    Assertions.assertTrue(contents.length() > 0);
  }
}
