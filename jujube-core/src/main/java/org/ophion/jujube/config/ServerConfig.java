package org.ophion.jujube.config;

import org.apache.hc.core5.function.Callback;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.nio.AsyncServerExchangeHandler;
import org.apache.hc.core5.http.nio.ssl.BasicServerTlsStrategy;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.LookupRegistry;
import org.apache.hc.core5.http.protocol.UriPatternOrderedMatcher;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.http2.ssl.H2ServerTlsStrategy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.conscrypt.Conscrypt;
import org.ophion.jujube.internal.JujubeExceptionCallback;
import org.ophion.jujube.internal.util.DataSize;
import org.ophion.jujube.tls.LazySecurePortStrategy;
import org.ophion.jujube.tls.SelfSignedCertificate;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

public class ServerConfig {
  private IOReactorConfig ioReactorConfig;
  private H2Config h2Config = H2Config.DEFAULT;
  private long multiPartPostLimitInBytes = DataSize.megabytes(200).toBytes();
  private HttpVersionPolicy versionPolicy = HttpVersionPolicy.NEGOTIATE;
  private Timeout handshakeTimeout;
  private LookupRegistry<Supplier<AsyncServerExchangeHandler>> lookupRegistry;
  private int listenPort;
  private Callback<Exception> exceptionCallback;
  private TlsStrategy tlsStrategy;

  public ServerConfig() {
    this.ioReactorConfig = IOReactorConfig.custom()
      .setSoReuseAddress(true)
      .setSoTimeout(Timeout.of(30, TimeUnit.SECONDS))
      .build();

    this.handshakeTimeout = Timeout.of(30, TimeUnit.SECONDS);
    this.lookupRegistry = new UriPatternOrderedMatcher<>();
    this.listenPort = 8080;
    this.exceptionCallback = new JujubeExceptionCallback();

    try {
      var certificate = new SelfSignedCertificate("jujube.local");
      var password = "password".toCharArray();

      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(null);
      ks.setKeyEntry("alias", certificate.key(), password, new java.security.cert.Certificate[]{certificate.cert()});

      SSLContext tlsContext = SSLContextBuilder.create()
        .setProvider(Conscrypt.newProvider())
        .loadKeyMaterial(ks, password)
        .build();

      this.tlsStrategy = new H2ServerTlsStrategy(tlsContext, new LazySecurePortStrategy(() -> new int[]{this.listenPort}));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public long getMultiPartPostLimitInBytes() {
    return multiPartPostLimitInBytes;
  }

  public void setMultiPartPostLimitInBytes(long multiPartPostLimitInBytes) {
    this.multiPartPostLimitInBytes = multiPartPostLimitInBytes;
  }

  public IOReactorConfig getIoReactorConfig() {
    return ioReactorConfig;
  }

  public void setIoReactorConfig(IOReactorConfig ioReactorConfig) {
    this.ioReactorConfig = ioReactorConfig;
  }

  public H2Config getH2Config() {
    return h2Config;
  }

  public void setH2Config(H2Config h2Config) {
    this.h2Config = h2Config;
  }

  public HttpVersionPolicy getVersionPolicy() {
    return versionPolicy;
  }

  public void setVersionPolicy(HttpVersionPolicy versionPolicy) {
    this.versionPolicy = versionPolicy;
  }

  public Timeout getHandshakeTimeout() {
    return handshakeTimeout;
  }

  public void setHandshakeTimeout(Timeout handshakeTimeout) {
    this.handshakeTimeout = handshakeTimeout;
  }

  public LookupRegistry<Supplier<AsyncServerExchangeHandler>> getLookupRegistry() {
    return lookupRegistry;
  }

  public void setLookupRegistry(LookupRegistry<Supplier<AsyncServerExchangeHandler>> lookupRegistry) {
    this.lookupRegistry = lookupRegistry;
  }

  public int getListenPort() {
    return listenPort;
  }

  public void setListenPort(int listenPort) {
    this.listenPort = listenPort;
  }

  public Callback<Exception> getExceptionCallback() {
    return exceptionCallback;
  }

  public void setExceptionCallback(Callback<Exception> exceptionCallback) {
    this.exceptionCallback = exceptionCallback;
  }

  public TlsStrategy getTlsStrategy() {
    return tlsStrategy;
  }

  public void setTlsStrategy(TlsStrategy tlsStrategy) {
    this.tlsStrategy = tlsStrategy;
  }

  public void disableTls() {
    setTlsStrategy(new BasicServerTlsStrategy(localAddress -> false));
  }
}
