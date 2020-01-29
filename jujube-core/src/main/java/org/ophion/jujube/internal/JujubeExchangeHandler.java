package org.ophion.jujube.internal;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.*;
import org.apache.hc.core5.http.nio.support.BasicResponseProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.ContentAwareEntityConsumer;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.response.DefaultHttpResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JujubeExchangeHandler implements AsyncServerExchangeHandler {
  private static final Logger LOG = Loggers.build();
  private final JujubeConfig config;
  private AsyncEntityConsumer<HttpEntity> consumer;
  private BasicResponseProducer producer;
  private volatile CapacityChannel inputCapacityChannel;
  private volatile DataStreamChannel outputDataChannel;
  private volatile boolean endStream;

  public JujubeExchangeHandler(JujubeConfig config) {
    this.config = config;
  }

  @Override
  public void handleRequest(HttpRequest request, EntityDetails entityDetails, ResponseChannel responseChannel, HttpContext context) throws HttpException, IOException {

    // wiring entity consumers with callbacks for processing continuation when ready:
    consumer = new ContentAwareEntityConsumer(this.config);
    AtomicReference<HttpResponse> response = new AtomicReference<>(null);

    consumer.streamStart(entityDetails, new FutureCallback<>() {
      @Override
      public void completed(HttpEntity result) {
        LOG.info("completed, handling should begin");

        try {
          response.compareAndSet(null, new org.ophion.jujube.response.HttpResponse("hello world!"));
          producer = new BasicResponseProducer(response.get());
          producer.sendResponse(responseChannel, context);

        } catch (HttpException | IOException e) {
          LOG.error("error onCompleted", e);
        }

      }

      @Override
      public void failed(Exception ex) {
        endStream = true;
        response.set(new DefaultHttpResponse(413));
      }

      @Override
      public void cancelled() {
        LOG.info("cancelled");
      }
    });

  }

  @Override
  public void failed(Exception cause) {
  }

  @Override
  public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
    LOG.info("update capacity");
    if (!endStream) {
      consumer.updateCapacity(capacityChannel);
      inputCapacityChannel = capacityChannel;
    } else {
      capacityChannel.update(0);
    }

  }

  @Override
  public void consume(ByteBuffer src) throws IOException {
    if (!endStream) {
      consumer.consume(src);
    } else {
      LOG.debug("skipping bytes");
    }
  }

  @Override
  public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
    LOG.info("stream end");
    consumer.streamEnd(trailers);
  }

  @Override
  public int available() {
    return producer.available();
  }

  @Override
  public void produce(DataStreamChannel channel) throws IOException {
    outputDataChannel = channel;
    LOG.info("produce");
    producer.produce(channel);
    if (endStream) {
      channel.endStream();
    }
  }

  @Override
  public void releaseResources() {
    if (consumer != null) {
      consumer.releaseResources();
    }

    if (producer != null) {
      producer.releaseResources();
    }
  }
}
