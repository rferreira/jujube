package org.ophion.jujube.config;

import org.apache.hc.core5.http.nio.ssl.SecurePortStrategy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Supplier;

public class LazySecurePortStrategy implements SecurePortStrategy {
  private final Supplier<int[]> securePortSupplier;

  public LazySecurePortStrategy(Supplier<int[]> securePortSupplier) {
    this.securePortSupplier = securePortSupplier;
  }

  @Override
  public boolean isSecure(SocketAddress localAddress) {
    final int port = ((InetSocketAddress) localAddress).getPort();
    for (final int securePort : securePortSupplier.get()) {
      if (port == securePort) {
        return true;
      }
    }
    return false;
  }
}
