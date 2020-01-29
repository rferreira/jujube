package org.ophion.jujube.internal;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ContentAwareRequestConsumer<T> implements AsyncRequestConsumer<Message<HttpRequest, T>> {
  private static final Logger LOG = Loggers.build();
  private final AtomicReference<Exception> exceptionHolder;
  private AsyncEntityConsumer<T> consumer;
  private JujubeConfig config;
  private long bytesProcessed = 0;
  private FutureCallback<Message<HttpRequest, T>> resultCallback;
  private boolean isDiscarding = false;

  @SuppressWarnings("unchecked")
  public ContentAwareRequestConsumer(JujubeConfig config, EntityDetails details, AtomicReference<Exception> exceptionRef) {
    this.config = config;
    this.exceptionHolder = exceptionRef;

    consumer = (AsyncEntityConsumer<T>) new NoopEntityConsumer();

    if (details != null) {
      //TODO(raf): make this configurable with some sort of registry:
      if (details.getContentType() != null) {
        var contentType = ContentType.parse(details.getContentType());
        if (contentType.isSameMimeType(ContentType.MULTIPART_FORM_DATA)) {
          consumer = (AsyncEntityConsumer<T>) new MultipartEntityConsumer();
        } else {
          consumer = (AsyncEntityConsumer<T>) new SizeAwareEntityConsumer();
        }
      }
    }
  }

  @Override
  public void consumeRequest(HttpRequest request,
                             EntityDetails entityDetails,
                             HttpContext context,
                             FutureCallback<Message<HttpRequest, T>> resultCallback) throws HttpException, IOException {

    if (entityDetails != null) {
      this.resultCallback = resultCallback;

      consumer.streamStart(entityDetails, new FutureCallback<T>() {
        @Override
        public void completed(T body) {
          final Message<HttpRequest, T> result = new Message<>(request, body);
          if (resultCallback != null) {
            resultCallback.completed(result);
          }
        }

        @Override
        public void failed(Exception ex) {
          if (resultCallback != null) {
            resultCallback.failed(ex);
          }
        }

        @Override
        public void cancelled() {
          if (resultCallback != null) {
            resultCallback.cancelled();
          }
        }
      });
    } else {
      final Message<HttpRequest, T> result = new Message<>(request, null);
      if (resultCallback != null) {
        resultCallback.completed(result);
      }
    }
  }

  @Override
  public void failed(Exception cause) {
    releaseResources();
  }

  @Override
  public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
    consumer.updateCapacity(capacityChannel);
  }

  @Override
  public void consume(ByteBuffer src) throws IOException {
    if (isDiscarding) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("discarding {} bytes", src.limit());
      }
      return;
    }

    consumer.consume(src);
    bytesProcessed += src.limit();

    var limit = config.getServerConfig().getPostBodySizeLimit();

    if (bytesProcessed >= limit.toBytes()) {
      var message = String.format("> ERROR: request body size limit exceeded, stopped after processing %s and limit is %s - aborting request.",
        DataSize.bytes(bytesProcessed), limit);
      System.err.println(message);
      exceptionHolder.set(new PostSizeLimitExceeded(message));

      // discarding remaining bits
      isDiscarding = true;
      releaseResources();
      resultCallback.completed(null);
    }
  }

  @Override
  public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
    consumer.streamEnd(trailers);
  }

  @Override
  public void releaseResources() {
    consumer.releaseResources();
  }
}
