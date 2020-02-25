package org.ophion.jujube.internal.consumers;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Content request consumer that limits the number of bytes processing and delegates processing to content-aware
 * consumers.
 */
public class ContentAwareRequestConsumer extends BasicRequestConsumer<HttpEntity> {
  private static final Logger LOG = Loggers.build();
  private final AtomicReference<Exception> exceptionHolder;
  private JujubeConfig config;
  private long bytesProcessed = 0;
  private boolean isDiscarding = false;

  @SuppressWarnings("unchecked")
  public ContentAwareRequestConsumer(JujubeConfig config, EntityDetails details, AtomicReference<Exception> exceptionRef) {
    super(() -> {
      if (details != null) {
        boolean isContentGreaterThanLimit = details.getContentLength() > config.getServerConfig().getRequestEntityLimit().toBytes();

        if (!isContentGreaterThanLimit && details.getContentType() != null) {
          var contentType = ContentType.parse(details.getContentType());
          if (contentType.isSameMimeType(ContentType.MULTIPART_FORM_DATA)) {
            return new MultipartEntityConsumer();
          } else {
            return new SizeAwareEntityConsumer();
          }
        }
      }

      //noinspection rawtypes
      return (AsyncEntityConsumer) new NoopEntityConsumer();
    });
    this.config = config;
    this.exceptionHolder = exceptionRef;

  }

  @Override
  public void consume(ByteBuffer src) throws IOException {
    //TODO: at some point we could give users the control of whether this should discard of just release
    // resources and rop the connection
    if (isDiscarding) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("discarding {} bytes", src.limit());
      }
      return;
    }
    // we need to do this here before consuming the byte buffer:
    bytesProcessed += src.limit();
    super.consume(src);

    LOG.debug("consumed {}", bytesProcessed);
    var limit = config.getServerConfig().getRequestEntityLimit();

    if (bytesProcessed > limit.toBytes()) {
      var message = String.format("> ERROR: request entity size limit exceeded, stopped after processing %s and limit is %s - aborting request.",
        DataSize.bytes(bytesProcessed), limit);
      System.err.println(message);
      exceptionHolder.set(new RequestEntityLimitExceeded(message));

      // discarding remaining bits
      isDiscarding = true;
    }
  }
}
