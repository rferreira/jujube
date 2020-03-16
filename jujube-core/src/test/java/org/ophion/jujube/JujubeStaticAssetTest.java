package org.ophion.jujube;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.http.ContentTypes;
import org.ophion.jujube.route.StaticAssetRouterHandler;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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

  @Test
  void shouldIncludeLastModifiedHeader() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();
    var lastModifiedAsString = client.execute(new HttpHead(endpoint.resolve("/static/")), response -> response.getHeader(HttpHeaders.LAST_MODIFIED).getValue());
    var instant = DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModifiedAsString);
    Assertions.assertNotNull(instant);
  }

  @Test
  void shouldSupportIfModifiedSince() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();

    // getting last modification date:
    var lastModifiedAsString = client.execute(new HttpHead(endpoint.resolve("/static/")), response -> response.getHeader(HttpHeaders.LAST_MODIFIED).getValue());
    var lastModificationDate = DateUtils.parseDate(lastModifiedAsString).toInstant();

    // a request with a last modification date before the header date should return a 304:
    var req = new HttpGet(endpoint.resolve("/static/index.html"));
    req.setHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(Date.from(lastModificationDate.plus(1, ChronoUnit.HOURS))));
    var code = client.execute(req, HttpResponse::getCode);
    Assertions.assertEquals(304, code);

    // after should return a 200:
    req.setHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(Date.from(lastModificationDate.minus(1, ChronoUnit.HOURS))));
    code = client.execute(req, HttpResponse::getCode);
    Assertions.assertEquals(200, code);
  }

  @Test
  void shouldSupportIfUnModifiedSince() throws IOException {
    config.route("/static/*", new StaticAssetRouterHandler("/assets/", "index.html"));
    server.start();

    // getting last modification date:
    var lastModifiedAsString = client.execute(new HttpHead(endpoint.resolve("/static/")), response -> response.getHeader(HttpHeaders.LAST_MODIFIED).getValue());
    var lastModificationDate = DateUtils.parseDate(lastModifiedAsString).toInstant();

    // a request with a last modification date after the header date should return a 412:
    var req = new HttpGet(endpoint.resolve("/static/index.html"));
    req.setHeader(HttpHeaders.IF_UNMODIFIED_SINCE, DateUtils.formatDate(Date.from(lastModificationDate.minus(1, ChronoUnit.HOURS))));
    var code = client.execute(req, HttpResponse::getCode);
    Assertions.assertEquals(412, code);

    // after should return a 200:
    req.setHeader(HttpHeaders.IF_UNMODIFIED_SINCE, DateUtils.formatDate(Date.from(lastModificationDate.plus(1, ChronoUnit.HOURS))));
    code = client.execute(req, HttpResponse::getCode);
    Assertions.assertEquals(200, code);
  }
}
