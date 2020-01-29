package org.ophion.jujube;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.MultipartEntityConsumer;
import org.ophion.jujube.internal.PostSizeLimitExceeded;
import org.ophion.jujube.internal.SizeAwareEntityConsumer;
import org.ophion.jujube.util.DataSize;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class ContentAwareEntityConsumer implements AsyncEntityConsumer<HttpEntity> {
  private final JujubeConfig config;
  private AsyncEntityConsumer<HttpEntity> consumer;
  private long bytesProcessed = 0;
  private FutureCallback<HttpEntity> resultCallback;

  public ContentAwareEntityConsumer(JujubeConfig config) {
    this.config = config;
  }

  @Override
  public void streamStart(EntityDetails entityDetails, FutureCallback<HttpEntity> resultCallback) throws HttpException, IOException {
    this.resultCallback = resultCallback;
    if (entityDetails != null) {
      //TODO(raf): make this configurable with some sort of registry:
      if (entityDetails.getContentType() != null) {
        var contentType = ContentType.parse(entityDetails.getContentType());
        if (contentType.isSameMimeType(ContentType.MULTIPART_FORM_DATA)) {
          consumer = new MultipartEntityConsumer();
        } else {
          consumer = new SizeAwareEntityConsumer();
        }
      }
    } else {
      consumer = (AsyncEntityConsumer) new NoopEntityConsumer();
    }

    consumer.streamStart(entityDetails, resultCallback);
  }

  @Override
  public void failed(Exception cause) {
    resultCallback.failed(cause);
  }

  @Override
  public HttpEntity getContent() {
    return consumer.getContent();
  }

  @Override
  public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
    consumer.updateCapacity(capacityChannel);
  }

  @Override
  public void consume(ByteBuffer src) throws IOException {
    consumer.consume(src);
    bytesProcessed += src.limit();

    var limit = config.getServerConfig().getPostBodySizeLimit();
    if (bytesProcessed >= limit.toBytes()) {
      var message = String.format("> ERROR: request body size limit exceeded, stopped after processing %s and limit is %s - aborting request.",
        DataSize.bytes(bytesProcessed), limit);
      System.err.println(message);
      var ex = new PostSizeLimitExceeded(message);
      failed(ex);
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
