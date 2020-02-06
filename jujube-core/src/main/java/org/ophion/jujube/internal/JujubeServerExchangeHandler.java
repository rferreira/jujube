package org.ophion.jujube.internal;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.AbstractServerExchangeHandler;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.context.FileParameter;
import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.context.ParameterSource;
import org.ophion.jujube.context.PrimitiveParameter;
import org.ophion.jujube.http.MultipartEntity;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.HttpResponseRequestTooLarge;
import org.ophion.jujube.response.HttpResponseServerError;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeHttpResponse;
import org.ophion.jujube.route.RouteHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class JujubeServerExchangeHandler extends AbstractServerExchangeHandler<Message<HttpRequest, HttpEntity>> {
  private static final Logger LOG = Loggers.build();
  private final JujubeConfig config;
  private final RouteHandler handler;
  private AtomicReference<Exception> exceptionRef;

  public JujubeServerExchangeHandler(JujubeConfig config, RouteHandler handler) {
    this.config = config;
    this.handler = handler;
    this.exceptionRef = new AtomicReference<>();
  }

  @Override
  protected AsyncRequestConsumer<Message<HttpRequest, HttpEntity>> supplyConsumer(HttpRequest request, EntityDetails entityDetails, HttpContext context) throws HttpException {
    LOG.debug("handling request: {}", request.toString());

    return new ContentAwareRequestConsumer<>(config, entityDetails, exceptionRef);
  }

  @Override
  protected void handle(Message<HttpRequest, HttpEntity> requestMessage, AsyncServerRequestHandler.ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {
    try {
      JujubeHttpResponse response = null;
      final var executor = config.getExecutorService();
      final var ctx = new JujubeHttpContext(this.config, context);

      // if we errored before getting here, quickly exit:
      if (exceptionRef.get() != null) {
        var ex = exceptionRef.get();
        if (ex instanceof RequestEntityLimitExceeded) {
          response = new HttpResponseRequestTooLarge();
        } else {
          response = new HttpResponseServerError();
        }
      }

      // dispatching handler if we do not already have an exception/response
      if (response == null) {
        try {
          response = executor.submit(() -> {
            extractParameters(ctx, requestMessage.getHead(), requestMessage.getBody());
            return handler.handle(ctx);
          }).get();
        } catch (ExecutionException e) {
          if (e.getCause() instanceof JujubeHttpException) {
            response = (JujubeHttpResponse) ((JujubeHttpException) e.getCause()).toHttpResponse();
          } else {
            e.printStackTrace();
            response = new HttpResponseServerError();
          }
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
      throw new HttpException("error handling request", e);
    }
  }

  private void extractParameters(JujubeHttpContext context, HttpRequest req, HttpEntity entity) {
    try {
      URLEncodedUtils.parse(req.getUri(), StandardCharsets.UTF_8)
        .forEach(nvp -> context.setParameter(ParameterSource.QUERY, new PrimitiveParameter(nvp.getName(), nvp.getValue(), ContentType.TEXT_PLAIN)));
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
            .forEach(nvp -> context.setParameter(ParameterSource.FORM, new PrimitiveParameter(nvp.getName(), nvp.getValue(), ContentType.TEXT_PLAIN)));
        }

        if (ContentType.MULTIPART_FORM_DATA.isSameMimeType(contentType)) {
          var parts = ((MultipartEntity) entity).getParts();
          parts.forEach(p -> {
            if (p.isText()) {
              context.setParameter(ParameterSource.FORM, new PrimitiveParameter(p.getName(), p.getValue(), p.getContentType()));
            } else {
              context.setParameter(ParameterSource.FORM, new FileParameter(p.getName(), Paths.get(p.getValue()), p.getContentType(), p.getFilename()));
            }
          });
        }
      }
    } catch (IOException e) {
      LOG.error("error decoding form body", e);
    }
  }
}
