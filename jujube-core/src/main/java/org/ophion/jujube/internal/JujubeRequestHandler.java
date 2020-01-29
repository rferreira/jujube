package org.ophion.jujube.internal;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.nio.support.BasicResponseProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.ophion.jujube.JujubeHttpContext;
import org.ophion.jujube.ParameterSource;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.http.MultipartEntity;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.HttpResponseInternalEngineError;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeHttpResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class JujubeRequestHandler implements AsyncServerRequestHandler<Message<HttpRequest, HttpEntity>> {
  private static final Logger LOG = Loggers.build();
  private final Function<JujubeHttpContext, JujubeHttpResponse> handler;
  private final JujubeConfig config;
  private final AtomicReference<Exception> exceptionRef;

  public JujubeRequestHandler(JujubeConfig config, Function<JujubeHttpContext, JujubeHttpResponse> handler) {
    this.config = config;
    this.handler = handler;
    this.exceptionRef = new AtomicReference<>();
  }

  @Override
  public AsyncRequestConsumer<Message<HttpRequest, HttpEntity>> prepare(HttpRequest request, EntityDetails entityDetails, HttpContext context) throws HttpException {
    return new ContentAwareRequestConsumer<>(config, entityDetails, exceptionRef);
  }

  @Override
  public void handle(Message<HttpRequest, HttpEntity> requestObject, ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {
    try {
      JujubeHttpResponse response;

      // if we errored before getting here, quickly exit:
      if (exceptionRef.get() != null) {
        response = new HttpResponseInternalEngineError();
        responseTrigger.submitResponse(new BasicResponseProducer(response), context);
        return;
      }

      final var executor = config.getExecutorService();
      final var ctx = new JujubeHttpContext(context);

      // dispatching handler:
      try {
        response = executor.submit(() -> {
          extractParameters(ctx, requestObject.getHead(), requestObject.getBody());
          return handler.apply(ctx);
        }).get();
      } catch (ExecutionException e) {
        if (e.getCause() instanceof JujubeHttpException) {
          response = (JujubeHttpResponse) ((JujubeHttpException) e.getCause()).toHttpResponse();
        } else {
          throw new IllegalStateException(e.getCause());
        }
      }

      // handling response:
      AsyncEntityProducer entityProducer;

      if (response.getContent() instanceof Path) {
        entityProducer = AsyncEntityProducers.create(((Path) response.getContent()).toFile(), response.getContentType());
      } else if (response.getContent() instanceof String) {
        entityProducer = AsyncEntityProducers.create((String) response.getContent(), response.getContentType());
      } else if (response.getContent() == null) {
        entityProducer = null;
      } else {
        throw new IllegalStateException("Unsupported content type: " + response.getContent());
      }

      var responseProducer = AsyncResponseBuilder.create(response.getCode())
        .setEntity(entityProducer)
        .setVersion(response.getVersion())
        .setHeaders(response.getHeaders())
        .build();
      responseTrigger.submitResponse(responseProducer, context);

    } catch (Exception e) {
      LOG.error("error handling request", e);
      throw new HttpException();
    }

  }

  private void extractParameters(JujubeHttpContext context, HttpRequest req, HttpEntity entity) {
    try {
      URLEncodedUtils.parse(req.getUri(), StandardCharsets.UTF_8)
        .forEach(nvp -> context.setParameter(ParameterSource.QUERY, nvp.getName(), nvp.getValue()));
    } catch (URISyntaxException e) {
      LOG.error("error decoding query string", e);
    }

    try {
      if (entity != null) {
        context.setEntity(entity);
        final var contentType = context.getEntityContentType().orElseThrow(() -> new IllegalStateException("unable to identify content type for entity"));

        if (ContentType.APPLICATION_FORM_URLENCODED.isSameMimeType(contentType)) {
          // TODO: need to find a better way to handle this going forward since this double memory usage
          EntityUtils.parse(entity)
            .forEach(nvp -> context.setParameter(ParameterSource.FORM, nvp.getName(), nvp.getValue()));
        }

        if (ContentType.MULTIPART_FORM_DATA.isSameMimeType(contentType)) {
          var parts = ((MultipartEntity) entity).getParts();
          parts.forEach(p -> context.setParameter(ParameterSource.FORM, p.getName(), p.getValue()));
        }
      }
    } catch (IOException e) {
      LOG.error("error decoding form body", e);
    }
  }
}
