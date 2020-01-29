package org.ophion.jujube.internal;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.nio.entity.AbstractBinAsyncEntityConsumer;
import org.apache.hc.core5.util.Args;
import org.ophion.jujube.http.MultipartEntity;
import org.ophion.jujube.internal.multipart.MultipartChunkDecoder;
import org.ophion.jujube.internal.multipart.MultipartHandler;
import org.ophion.jujube.internal.multipart.PartMetadata;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipartEntityConsumer extends AbstractBinAsyncEntityConsumer<HttpEntity> {
  private static final Logger LOG = Loggers.build();
  private MultipartChunkDecoder decoder;
  private ContentType contentType;
  private List<MultipartEntity.Part> parts = new ArrayList<>();

  @Override
  protected void streamStart(ContentType contentType) throws IOException {
    try {
      this.contentType = contentType;
      var boundary = contentType.getParameter("boundary");
      Args.notNull(boundary, "boundary");

      decoder = new MultipartChunkDecoder(boundary, new MultipartHandler() {
        @Override
        public void onTextPart(PartMetadata metadata, String value) {
          parts.add(new MultipartEntity.Part(metadata.getName(), value, metadata.getFilename(), metadata.getContentType(), metadata.getHeaders()));
        }

        @Override
        public void onBinaryPart(PartMetadata metadata, Path contents) {
          parts.add(new MultipartEntity.Part(metadata.getName(), contents.toString(), metadata.getFilename(), metadata.getContentType(), metadata.getHeaders()));
        }
      });

    } catch (Exception ex) {
      LOG.error("error parsing multipart content", ex);
      throw new IOException(ex);
      // a read or write error occurred
    }
  }

  @Override
  protected HttpEntity generateContent() throws IOException {
    return new MultipartEntity(Collections.unmodifiableList(parts), this.contentType, null);
  }

  @Override
  protected int capacityIncrement() {
    return Integer.MAX_VALUE;
  }

  @Override
  protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
    decoder.decode(src.array(), 0, src.limit(), endOfStream);
  }

  @Override
  public void releaseResources() {
    LOG.debug("releasing resources");
  }
}
