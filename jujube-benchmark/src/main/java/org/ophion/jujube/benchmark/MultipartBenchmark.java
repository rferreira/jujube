package org.ophion.jujube.benchmark;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.openjdk.jmh.annotations.*;
import org.ophion.jujube.Jujube;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.util.DataSize;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Fork(value = 1, warmups = 1, jvmArgs = "-Xmx64m")
@BenchmarkMode({Mode.Throughput})
public class MultipartBenchmark {

  @Benchmark
  public void post(BenchmarkState state) throws IOException {

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RepeatingInputStream(DataSize.megabytes(state.payloadSizeInMB).toBytes()))
      .build();

    var request = new HttpPost(state.endpoint.resolve("/post"));
    request.setEntity(entity);

    state.client.execute(request, response -> {
      if (response.getCode() != 200) {
        System.exit(response.getCode());
      }
      return true;
    });
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    @Param({"1", "5", "10", "100"})
    public int payloadSizeInMB;
    private URI endpoint;
    private HttpClient client;
    private Jujube server;

    @Setup
    public void setup() throws KeyStoreException, NoSuchAlgorithmException, IOException, URISyntaxException, KeyManagementException {
      // building client
      final SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
        .build();
      final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
        .setSslContext(sslContext)
        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build();
      final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(sslSocketFactory)
        .build();

      client = HttpClients.custom()
        .disableAutomaticRetries()
        .setConnectionManager(cm)
        .build();

      var config = new JujubeConfig();
      config.route("/*", (req, ctx) -> new JujubeResponse(200));
      server = new Jujube(config);

      this.endpoint = URIBuilder.localhost().setPort(config.getServerConfig().getListenPort()).setScheme("https").build();

      server.start();
    }

    @TearDown
    public void teardown() throws IOException {
      this.server.stop();
    }
  }
}
