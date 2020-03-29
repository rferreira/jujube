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
import org.ophion.jujube.middleware.Middleware;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.Parameter;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.response.ResponseRequestTooLarge;
import org.ophion.jujube.response.ResponseServerError;
import org.ophion.jujube.routing.Route;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core exchanger (exchanges a request for a response) and dispatch logic.
 */
public class JujubeServerExchangeHandler extends AbstractServerExchangeHandler<Message<HttpRequest, HttpEntity>> {
  private static final Logger LOG = Loggers.build();
  private final JujubeConfig config;
  private final ExecutorService executor;
  private final Route route;
  private AtomicReference<Throwable> exceptionRef;

  public JujubeServerExchangeHandler(JujubeConfig config, Route route) {
    this.config = config;
    this.route = route;
    this.exceptionRef = new AtomicReference<>();
    this.executor = config.getExecutorService();
  }

  @Override
  protected AsyncRequestConsumer<Message<HttpRequest, HttpEntity>> supplyConsumer(HttpRequest request, EntityDetails entityDetails, HttpContext context) {
    LOG.debug("handling request: {}", request.toString());

    // hydrating context:
    context.setAttribute("jujube.route", route);
    context.setAttribute("jujube.config", config);

    LOG.debug("dispatching pre consumption middleware");
    Future<?> future = executor.submit(() -> {
      for (Middleware middleware : config.getMiddlewareRegistry()) {
        try {
          middleware.onBeforeContentConsumption(request, context);
        } catch (Exception e) {
          LOG.error("error while dispatching middleware {}", middleware);
          e.printStackTrace();
          exceptionRef.set(e);
        }
      }
    });

    try {
      future.get();
    } catch (InterruptedException | ExecutionException ex) {
      throw new IllegalStateException(ex);
    }

    return new ContentAwareRequestConsumer(config, entityDetails, exceptionRef);
  }

  @Override
  protected void handle(Message<HttpRequest, HttpEntity> requestMessage, AsyncServerRequestHandler.ResponseTrigger responseTrigger, HttpContext context) throws HttpException {
    try {
      // wrapping around jujube objects:
      JujubeResponse response = null;
      JujubeRequest request = new JujubeRequest(requestMessage.getHead(), requestMessage.getBody());

      // dispatching handler if we do not already have an exception/response
      if (exceptionRef.get() == null) {
        Future<JujubeResponse> responseFuture = this.executor.submit(() -> {

          // extracting parameters:
          var parentRequest = requestMessage.getHead();
          var entity = requestMessage.getBody();

          // constraints
          List<Parameter> parameters = new ArrayList<>();

          // extracting parameters:
          config.getParameterExtractorRegistry().forEach(pe -> {
            parameters.addAll(pe.extract(parentRequest, entity, context));
          });

          request.setParameters(parameters);

          // dispatching
          return route.getHandler().handle(request, context);

          // blocking and awaiting response:
        });

        try {
          response = responseFuture.get();
        } catch (ExecutionException e) {
          exceptionRef.set(e);
        }
      }

      // if we errored before getting here, quickly exit:
      if (exceptionRef.get() != null) {
        response = handleException(exceptionRef.get());
      }

      // handling response:
      if (response != null) {

        LOG.debug("dispatching post-execution middleware");
        JujubeResponse finalResponse = response;
        Future<?> future = executor.submit(() -> {
          for (Middleware middleware : config.getMiddlewareRegistry()) {
            try {
              middleware.onAfterHandler(request, finalResponse, context);
            } catch (Exception e) {
              LOG.error("error while dispatching middleware {}", middleware);
              e.printStackTrace();
              exceptionRef.set(e);
            }
          }
        });

        try {
          future.get();
        } catch (InterruptedException | ExecutionException ex) {
          throw new IllegalStateException(ex);
        }

        send(responseTrigger, response, context);
      }

    } catch (Exception e) {
      LOG.error("error handling request", e);
      throw new HttpException("error handling request", e);
    }
  }

  private JujubeResponse handleException(Throwable ex) {
    JujubeResponse response;

    if (ex instanceof ExecutionException) {
      ex = ex.getCause();
    }

    if (ex instanceof RequestEntityLimitExceeded) {
      response = new ResponseRequestTooLarge();
    } else if (ex instanceof JujubeHttpException) {
      response = ((JujubeHttpException) ex).toHttpResponse();
    } else {
      ex.printStackTrace();
      response = new ResponseServerError();
    }

    return response;
  }

  private void send(AsyncServerRequestHandler.ResponseTrigger responseTrigger, JujubeResponse response, HttpContext context) throws IOException, HttpException {
    LOG.debug("beginning to send response {}", response);
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

  }
}
