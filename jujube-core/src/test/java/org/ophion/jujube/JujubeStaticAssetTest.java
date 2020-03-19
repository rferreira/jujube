package org.ophion.jujube;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.http.ContentTypes;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.route.StaticAssetRouteHandler;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class JujubeStaticAssetTest extends IntegrationTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldHandleStaticAssetsFromClasspath() throws IOException {
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
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
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
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
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
    server.start();
    var code = client.execute(new HttpGet(endpoint.resolve("/static/../assets-not-reachable.xml")), HttpResponse::getCode);
    Assertions.assertEquals(404, code);
  }

  @Test
  void should415OnInvalidMethods() throws IOException {
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
    server.start();
    var code = client.execute(new HttpPost(endpoint.resolve("/static/hello.txt")), HttpResponse::getCode);
    Assertions.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, code);
  }

  @Test
  void shouldLoadIndexFile() throws IOException {
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
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
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
    server.start();
    var lastModifiedAsString = client.execute(new HttpHead(endpoint.resolve("/static/")), response -> response.getHeader(HttpHeaders.LAST_MODIFIED).getValue());
    var instant = DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModifiedAsString);
    Assertions.assertNotNull(instant);
  }

  @Test
  void shouldSupportIfModifiedSince() throws IOException {
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
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
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
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

  @Test
  @Disabled("used for profiling")
  void performance() throws InterruptedException {
    int numThreads = 100;
    int requestsPerThread = 10_0000;
    HttpClient customClient;
    try {
      final SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
        .build();
      final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
        .setSslContext(sslContext)
        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .setTlsVersions(TLS.V_1_2)
        .build();
      final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(sslSocketFactory)
        .setMaxConnPerRoute(numThreads)
        .build();

      customClient = HttpClients.custom()
        .disableAutomaticRetries()
        .setDefaultRequestConfig(RequestConfig.custom()
          .setResponseTimeout(60, TimeUnit.SECONDS)
          .setConnectTimeout(1, TimeUnit.SECONDS)
          .build())
        .setConnectionManager(cm)
        .build();
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      throw new IllegalStateException(e);
    }

    var latch = new CountDownLatch(1);
    ExecutorService es = new ThreadPoolExecutor(numThreads, numThreads, Long.MAX_VALUE, TimeUnit.HOURS, new SynchronousQueue<>());
    IntStream.rangeClosed(0, numThreads - 1).forEach(t -> {
      es.submit(() -> {
        try {
          latch.await();
          for (int i = 0; i < requestsPerThread; i++) {
            LOG.info("thread {} request {}", t + 1, i + 1);
            var contents = customClient.execute(new HttpGet(endpoint.resolve("/static/index.html")), response -> {
              Assertions.assertEquals(200, response.getCode());
              Assertions.assertEquals(ContentType.TEXT_HTML.withCharset(config.getDefaultCharset()).toString(), response.getEntity().getContentType());
              return EntityUtils.toString(response.getEntity());
            });
            Assertions.assertTrue(contents.length() > 0);
          }
        } catch (IOException | InterruptedException e) {
          throw new IllegalStateException(e);
        }
      });
    });

    // starting server
    config.route("/static/*", new StaticAssetRouteHandler("/assets/", "index.html"));
    server.start();

    // starting test:
    latch.countDown();

    es.awaitTermination(5, TimeUnit.MINUTES);
  }
}
