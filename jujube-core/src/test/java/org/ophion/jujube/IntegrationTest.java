package org.ophion.jujube;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ophion.jujube.config.JujubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class IntegrationTest {
  private static Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

  HttpClient client;
  JujubeConfig config;
  Jujube server;
  URI endpoint;


  IntegrationTest() {
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
        .build();

      client = HttpClients.custom()
        .disableAutomaticRetries()
        .setConnectionManager(cm)
        .build();
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeEach
  void setUp() throws UnknownHostException, URISyntaxException {
    LOG.debug("creating server");
    config = new JujubeConfig();
    config.getServerConfig().setListenPort(new Random().nextInt(1_000) + 8_000);
    config.getServerConfig().setIoReactorConfig(IOReactorConfig
      .custom()
      .setSelectInterval(TimeValue.of(100, TimeUnit.MILLISECONDS))
      .build()
    );
    server = new Jujube(config);
    endpoint = URIBuilder.localhost().setPort(config.getServerConfig().getListenPort()).setScheme("https").build();
  }

  @AfterEach
  void tearDown() {
    LOG.debug("shutting down server");
    server.stop();
  }

}
