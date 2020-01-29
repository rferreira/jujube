package org.ophion.jujube.internal;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.support.classic.AbstractClassicServerExchangeHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.JujubeHttpException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JujubeClassicHandler extends AbstractClassicServerExchangeHandler {
  private static final Logger LOG = Loggers.build();

  public JujubeClassicHandler(JujubeConfig config) {
    super(8 * 1024, config.getExecutorService());
  }

  @Override
  protected void handle(HttpRequest request, InputStream requestStream, HttpResponse response, OutputStream responseStream, HttpContext context) throws IOException, HttpException {
    LOG.debug("Response: {}", response);

    response = new JujubeHttpException(413).toHttpResponse();

  }
}
