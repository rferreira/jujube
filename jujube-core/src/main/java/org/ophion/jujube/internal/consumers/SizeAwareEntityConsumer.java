package org.ophion.jujube.internal.consumers;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.nio.entity.AbstractBinAsyncEntityConsumer;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.internal.util.TieredOutputStream;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class SizeAwareEntityConsumer extends AbstractBinAsyncEntityConsumer<HttpEntity> {
  private static final Logger LOG = Loggers.build();
  private final TieredOutputStream buffer;
  private ContentType contentType;

  public SizeAwareEntityConsumer() {
    buffer = new TieredOutputStream(DataSize.mebibytes(5), DataSize.megabytes(100));
  }

  @Override
  protected void streamStart(ContentType contentType) throws HttpException, IOException {
    LOG.debug("processing stream with content type:" + contentType);
    this.contentType = contentType;
  }

  @Override
  protected HttpEntity generateContent() {
    return new AbstractHttpEntity(contentType, null) {
      @Override
      public InputStream getContent() throws IOException, UnsupportedOperationException {
        return buffer.getContentAsStream();
      }

      @Override
      public boolean isStreaming() {
        return false;
      }

      @Override
      public void close() throws IOException {
        buffer.close();
      }

      @Override
      public long getContentLength() {
        return buffer.getSize();
      }
    };
  }

  @Override
  protected int capacityIncrement() {
    return (int) buffer.getLimit().toBytes();
  }

  @Override
  protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
    buffer.write(src.array(), src.arrayOffset() + src.position(), src.remaining());
    src.clear();
  }

  @Override
  public void releaseResources() {
    LOG.debug("releasing resources");
    try {
      buffer.close();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
