package org.ophion.jujube.internal;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.AbstractServerExchangeHandler;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.consumers.ContentAwareRequestConsumer;
import org.ophion.jujube.internal.consumers.RequestEntityLimitExceeded;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.Parameter;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.response.ResponseRequestTooLarge;
import org.ophion.jujube.response.ResponseServerError;
import org.ophion.jujube.routing.Route;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core exchanger (exchanges a request for a response) and dispatch logic.
 */
public class JujubeServerExchangeHandler extends AbstractServerExchangeHandler<Message<HttpRequest, HttpEntity>> {
  private static final Logger LOG = Loggers.build();
  private final JujubeConfig config;
  private final ExecutorService executor;
  private final Route route;
  private AtomicReference<Exception> exceptionRef;

  public JujubeServerExchangeHandler(JujubeConfig config, Route route) {
    this.config = config;
    this.route = route;
    this.exceptionRef = new AtomicReference<>();
    this.executor = config.getExecutorService();
  }

  @Override
  protected AsyncRequestConsumer<Message<HttpRequest, HttpEntity>> supplyConsumer(HttpRequest request, EntityDetails entityDetails, HttpContext context) {
    LOG.debug("handling request: {}", request.toString());
    return new ContentAwareRequestConsumer(config, entityDetails, exceptionRef);
  }

  @Override
  protected void handle(Message<HttpRequest, HttpEntity> requestMessage, AsyncServerRequestHandler.ResponseTrigger responseTrigger, HttpContext context) throws HttpException {
    try {

      JujubeResponse response = null;

      // if we errored before getting here, quickly exit:
      if (exceptionRef.get() != null) {
        var ex = exceptionRef.get();
        if (ex instanceof RequestEntityLimitExceeded) {
          response = new ResponseRequestTooLarge();
        } else {
          response = new ResponseServerError();
        }
      }

      // dispatching handler if we do not already have an exception/response
      if (response == null) {
        try {

          response = this.executor.submit(() -> {
            // hydrating request:
            var entity = requestMessage.getBody();
            var parentRequest = requestMessage.getHead();
            context.setAttribute("jujube.route", route);

            // constraints
            List<Parameter> parameters = new ArrayList<>();

            // extracting parameters:
            config.getParameterExtractors().forEach(pe -> {
              parameters.addAll(pe.extract(parentRequest, entity, context));
            });

            var req = new JujubeRequest(parentRequest, entity, parameters);

            // dispatching
            return route.getHandler().handle(req, context);

            // blocking and awaiting response:
          }).get();

        } catch (ExecutionException e) {
          if (e.getCause() instanceof JujubeHttpException) {
            response = (JujubeResponse) ((JujubeHttpException) e.getCause()).toHttpResponse();
          } else {
            e.printStackTrace();
            response = new ResponseServerError();
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
}
