package org.ophion.jujube.internal.multipart;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.util.Args;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.internal.util.TieredOutputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A near zero copy, streaming, multipart form post decoder. RFC-7578 compliant.
 * Features:
 * - Chunk size independence and streaming friendly, so it can be used easily with NIO channels.
 * - Fixed memory usage (configurable and zero-copied)
 * - Reasonable fast at about 60% of your disk write speed.
 * <p>
 * Limitations:
 * - No support for multipart/nested
 * - No support for multipart/mixed
 * <p>
 * <p>
 * This class is non re-entrant, we discard any bytes submitted post EPILOGUE segment identification.
 * Further improvements:
 * - According to profile data, ByteBuffer methods are not being in-lined as much as one would hope with calls to
 * .hasRemaining() consuming 9% of the processing time. Conceivably, we could move to using direct byte arrays, but my
 * fear is that it will make the code very hard to follow.
 * <p>
 * Relevant reading:
 * https://tools.ietf.org/html/rfc7578
 */
@Contract(threading = ThreadingBehavior.UNSAFE)
public class MultipartChunkDecoder implements AutoCloseable {
  private static final Logger LOG = Loggers.build();
  private static final byte[] TWO_DASHES_CRLF = "--\r\n".getBytes(StandardCharsets.US_ASCII);
  private final ByteBuffer buffer;
  private final MultipartHandler handler;
  private final int minBytesNeeded;
  private final byte[][] delimiters = new byte[Segment.values().length][];
  private Segment currentSegment;
  private TieredOutputStream currentPartBodyAccumulator;
  private TieredOutputStream currentHeaderAccumulator;
  private PartMetadata currentPartMetadata;
  private Charset charsetIfNoneProvided;
  private MultipartChunkDecoderConfig config;
  private Set<AutoCloseable> resourcesToClose = new HashSet<>();

  public MultipartChunkDecoder(String boundary, MultipartHandler handler) {
    this(boundary, handler, new MultipartChunkDecoderConfig());
  }

  public MultipartChunkDecoder(String boundary, MultipartHandler handler, MultipartChunkDecoderConfig config) {
    Args.notBlank(boundary, "boundary cannot be empty");
    Args.notNull(handler, "handler cannot be empty");
    Args.check(boundary.length() <= 70, "spec prohibits boundaries greater than 70 characters");
    Args.check(config.getBufferSize().toBytes() >= boundary.length(), "buffer size must exceed boundary size");

    // delimiters:
    delimiters[Segment.PREAMBLE.ordinal()] = ("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII);
    delimiters[Segment.HEADER.ordinal()] = ("\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
    delimiters[Segment.BODY.ordinal()] = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);

    // we need at least enough bytes to account for the largest delimiter + 1 data byte.
    this.minBytesNeeded = delimiters[Segment.BODY.ordinal()].length + 3;

    // make this always larger than the boundary or throw an error
    this.buffer = ByteBuffer.allocate((int) config.getBufferSize().toBytes());
    this.currentSegment = Segment.PREAMBLE;
    this.handler = handler;
    this.config = config;
  }

  /**
   * Accumulate chunks into a minimal size (that way the chunk size doesn't really matter) and
   * once the buffer is of a certain size it processes it compacting said buffer when needed.
   * This is NOT thread safe.
   *
   * @param chunk       byte chunk of any size to process
   * @param isLastChunk whether this chunk represents the last chunk in this byte sequence or not.
   */
  public void decode(ByteBuffer chunk, boolean isLastChunk) throws IOException {
    if (chunk.limit() > buffer.capacity()) {
      throw new IllegalStateException("chunk size is greater than the internal buffer size, please try increasing the buffer size");
    }

    while (chunk.hasRemaining()) {
      if (buffer.remaining() >= chunk.remaining()) {
        buffer.put(chunk);
      } else {
        // copying as many bytes as we can
        var availableCapacity = buffer.remaining();
        buffer.put(chunk.slice().limit(availableCapacity));
        chunk.position(chunk.position() + availableCapacity);
      }
      if (buffer.remaining() == 0) {
        processBuffer(buffer.flip());
        buffer.compact();
      }
    }

    if (isLastChunk) {
      processBuffer(buffer.flip());
    }
  }

  /**
   * Given a {@link ByteBuffer}, process it, looking for message delimiters and triggering callbacks.
   *
   * @param contents contents to be decoded.
   * @throws IOException IO error if any.
   */
  private void processBuffer(ByteBuffer contents) throws IOException {

    if (currentSegment == Segment.EPILOGUE) {
      contents.position(contents.limit());
      return;
    }

    // everything below is quite a hot IO path so the fewer the instructions the better:
    while (contents.hasRemaining() && currentSegment != Segment.EPILOGUE) {

      // identifying the next limiter we would like to search for:
      byte[] currentDelimiter = delimiters[currentSegment.ordinal()];

      // if we have fewer available bytes than the minimal needed, refill buffer:
      if (this.minBytesNeeded > contents.remaining()) {
        break;
      }

      // from the current index, compare those bytes against the wanted delimiter - this is faster than a binary search:
      var hasReachedSegmentEnd = Arrays.equals(
        contents.array(), contents.position(), contents.position() + currentDelimiter.length,
        currentDelimiter, 0, currentDelimiter.length
      );

      var previousState = currentSegment;
      var nextSate = previousState;

      // always advance our position:
      final byte currentByte = contents.get();

      if (currentSegment == Segment.PREAMBLE) {
        if (hasReachedSegmentEnd) {
          nextSate = Segment.HEADER;
        } else {
          LOG.debug("skipping preamble");
        }
      }

      if (currentSegment == Segment.HEADER) {
        if (currentHeaderAccumulator == null) {
          currentHeaderAccumulator = new TieredOutputStream(config.getHeaderSizeLimit());
          resourcesToClose.add(currentHeaderAccumulator);
        }
        // if end limiter not found, consume everything:
        if (hasReachedSegmentEnd) {
          // this should be US ASCII but, but it looks like jetty uses UTF-8 so let's do the same:
          currentPartMetadata = new PartMetadata(currentHeaderAccumulator.getContentAsText(StandardCharsets.UTF_8));
          currentHeaderAccumulator = null;
          nextSate = Segment.BODY;
        } else {
          currentHeaderAccumulator.write(currentByte);
          // optimization: we accumulate until we encounter the first byte of the segment or the end of the buffer:
          accumulate(contents, currentDelimiter[0], currentHeaderAccumulator);
        }
      }

      if (currentSegment == Segment.BODY) {
        // if an accumulator doesn't exist, let's create one:
        if (currentPartBodyAccumulator == null) {
          currentPartBodyAccumulator = new TieredOutputStream(config.getBodySizeLimit());
          resourcesToClose.add(currentPartBodyAccumulator);
        }

        if (!hasReachedSegmentEnd) {
          currentPartBodyAccumulator.write(currentByte);
          // optimization: we accumulate until we encounter the first byte of the segment or the end of the buffer:
          accumulate(contents, currentDelimiter[0], currentPartBodyAccumulator);
        } else {
          if (currentPartMetadata.isText()) {

            // if part does not have a charset, but we have a default one, use it:
            var charset = currentPartMetadata.getContentType().getCharset();
            if (charset == null) {
              if (charsetIfNoneProvided != null) {
                LOG.debug("overwriting part charset to {}", charsetIfNoneProvided);
                charset = charsetIfNoneProvided;
              } else {
                // this should be US ASCII but, but it looks like jetty uses UTF-8 so let's do the same:
                charset = StandardCharsets.UTF_8;
              }
            }
            String textPart = currentPartBodyAccumulator.getContentAsText(charset);

            // A name with a value of '_charset_' indicates that the part is not an HTML field, but the default charset
            // to use for parts without explicit charset information.
            if (currentPartMetadata.getName().equals("_charset_")) {
              LOG.debug("default charset found, trying to honor it");
              try {
                charsetIfNoneProvided = Charset.forName(textPart);
              } catch (UnsupportedCharsetException ex) {
                throw new IllegalStateException("Unable to find charset " + textPart, ex);
              }
            }

            handler.onTextPart(currentPartMetadata, textPart);
          } else {
            handler.onBinaryPart(currentPartMetadata, currentPartBodyAccumulator.getContentsAsPath());
          }
          // resetting accumulator
          currentPartBodyAccumulator = null;

          // now we need to assess if we've reached the end body or final delimiter,
          // we do that by looking at the next 2 bytes:
          var currentSegmentEndIndex = contents.position() + currentDelimiter.length - 1;
          var hasReachedFinalDelimiter = Arrays.equals(
            contents.array(), currentSegmentEndIndex, currentSegmentEndIndex + TWO_DASHES_CRLF.length - 1,
            TWO_DASHES_CRLF, 0, TWO_DASHES_CRLF.length - 1
          );

          if (hasReachedFinalDelimiter) {
            // The final delimiter found, please note that we don't skip the extra two bytes since they are going to be
            // discarded anyways as preamble
            nextSate = Segment.EPILOGUE;
          } else {
            nextSate = Segment.HEADER;
          }
        }
      }

      // moving states forward:
      if (nextSate != currentSegment) {
        currentSegment = nextSate;
        LOG.debug("delimiter found for the segment {}, moving on to segment {}", previousState, currentSegment);
      }

      // skipping segment
      if (hasReachedSegmentEnd) {
        // the minus 1 accounts for the current byte already popped
        contents.position(contents.position() + currentDelimiter.length - 1);
      }
    }
  }

  /**
   * Copies bytes from source to destination until a byte we encounter a stop byte.
   * @param source the source to copy from.
   * @param stopByte the specific byte we want to stop once we find.
   * @param destination the stream to accumulate into.
   * @throws IOException any errors thrown by the OutputStream.
   */
  private void accumulate(ByteBuffer source, byte stopByte, OutputStream destination) throws IOException {
    byte nextByte = 0;
    while (source.hasRemaining()) {
      nextByte = source.get();

      if (nextByte == stopByte) {
        // rollback position since we went past the stopByte
        source.position(source.position() - 1);
        break;
      }
      destination.write(nextByte);
    }
  }

  @Override
  public void close() throws Exception {
    LOG.debug("decoder closing, closing {} other resources", resourcesToClose.size());
    for (AutoCloseable closeable : resourcesToClose) {
      closeable.close();
    }
    resourcesToClose.clear();
  }
}
