package org.ophion.jujube.internal.multipart;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.util.Args;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.internal.util.TieredOutputStream;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A near zero copy, streaming, multipart form post decoder. RFC-7578 compliant.
 * Features:
 * - Chunk size independence
 * - Fixed memory usage (configurable and zero-copied)
 * - Limits enforcement (TODO)
 * <p>
 * Limitations:
 * - No support for multipart/nested
 * - No support for multipart/mixed
 * <p>
 * <p>
 * This class is non re-entrant, any bytes submitted once the EPILOGUE segment is found are simply discarded.
 * <p>
 * Relevant reading:
 * https://tools.ietf.org/html/rfc7578
 */
@Contract(threading = ThreadingBehavior.UNSAFE)
public class MultipartChunkDecoder {
  private static final Logger LOG = Loggers.build();
  private static final byte[] TWO_DASHES_CRLF = "--\r\n".getBytes(StandardCharsets.US_ASCII);
  private final ByteBuffer buffer;
  private final MultipartHandler handler;
  private final int minBytesNeeded;
  private final Map<Segment, byte[]> delimiters = new HashMap<>();
  private Segment currentSegment;
  private TieredOutputStream currentPartBodyAccumulator;
  private TieredOutputStream headerAccumulator;
  private PartMetadata currentPartMetadata;
  private Charset charsetIfNoneProvided;
  private DataSize headerSizeLimit = DataSize.kibibytes(100);
  private DataSize bodySizeLimit = DataSize.megabytes(100);

  public MultipartChunkDecoder(String boundary, MultipartHandler handler) {
    this(boundary, handler, (int) DataSize.kibibytes(8).toBytes());
  }

  public MultipartChunkDecoder(String boundary, MultipartHandler handler, int bufferSizeInBytes) {
    Args.notBlank(boundary, "boundary cannot be empty");
    Args.notNull(handler, "handler cannot be empty");
    Args.check(boundary.length() <= 70, "spec prohibits boundaries greater than 70 characters");
    Args.check(bufferSizeInBytes >= boundary.length(), "buffer size must exceed boundary size");

    // delimiters:
    delimiters.put(Segment.PREAMBLE, ("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII));
    delimiters.put(Segment.HEADER, ("\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    delimiters.put(Segment.BODY, ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII));

    // we need at least enough bytes to account for the largest delimiter + 1 data byte.
    this.minBytesNeeded = delimiters.get(Segment.BODY).length + 3;
    this.buffer = ByteBuffer.allocate(bufferSizeInBytes); // make this always larger than the boundary or throw an error
    this.currentSegment = Segment.PREAMBLE;
    this.handler = handler;
  }

  /**
   * Accumulate chunks into a minimal size (that way the chunk size doesn't really matter) and
   * once the buffer is of a certain size it processes it compacting said buffer when needed.
   * This is NOT thread safe.
   *
   * @param chunk       byte chunk of any size to process
   * @param isLastChunk whether this chunk represents the last chunk in this byte sequence or not.
   */
  public void decode(byte[] chunk, int offset, int length, boolean isLastChunk) throws IOException {

    if (chunk.length > buffer.capacity()) {
      throw new IllegalStateException("chunk size is greater than the internal buffer size, please try increasing the buffer size");
    }

    var wrappedChunk = ByteBuffer.wrap(chunk, offset, length);

    while (wrappedChunk.hasRemaining()) {
      if (buffer.remaining() >= wrappedChunk.remaining()) {
        buffer.put(wrappedChunk);
      } else {
        // copying as many bytes as we can
        var availableCapacity = buffer.remaining();
        buffer.put(wrappedChunk.slice().limit(availableCapacity));
        wrappedChunk.position(wrappedChunk.position() + availableCapacity);
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
   * Given a {link ByteBuffer}, process it, looking for message delimiters.
   *
   * @param contents contents to be decoded.
   * @throws IOException IO error if any.
   */
  private void processBuffer(ByteBuffer contents) throws IOException {

    //TODO: check limits here
    if (currentSegment == Segment.EPILOGUE) {
      contents.position(contents.limit());
      return;
    }

    while (contents.hasRemaining() && currentSegment != Segment.EPILOGUE) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("position {}/{}", contents.position(), contents.limit());
        LOG.trace("buffer contents:\n{}", new String(contents.array(), contents.position(), contents.limit(), StandardCharsets.US_ASCII));
      }

      // check limits to prevent exploits:
      checkLimits();

      // identifying the next limiter we would like to search for:
      byte[] currentDelimiter = delimiters.get(currentSegment);

      // if we have fewer available bytes than the minimal needed, refill buffer:
      if (this.minBytesNeeded > contents.remaining()) {
        if (LOG.isTraceEnabled()) {
          LOG.debug("breaking early to refill buffer");
        }
        break;
      }

      // from the current index, compare those bytes against the wanted delimiter - this is faster than a binary search:
      var hasReachedSegmentEnd = Arrays.equals(
        contents.array(), contents.position(), contents.position() + currentDelimiter.length,
        currentDelimiter, 0, currentDelimiter.length
      );

      var previousState = currentSegment;
      var nextSate = previousState;

      byte currentByte = contents.get();

      if (currentSegment == Segment.PREAMBLE) {
        if (hasReachedSegmentEnd) {
          nextSate = Segment.HEADER;
        } else {
          LOG.debug("skipping preamble");
        }
      }

      if (currentSegment == Segment.HEADER) {
        if (headerAccumulator == null) {
          headerAccumulator = new TieredOutputStream(this.headerSizeLimit, DataSize.kibibytes(0));
        }
        // if end limiter not found, consume everything:
        if (hasReachedSegmentEnd) {
          // this should be US ASCII but, but it looks like jetty uses UTF-8 so let's do the same:
          currentPartMetadata = new PartMetadata(headerAccumulator.getContentAsText(StandardCharsets.UTF_8));
          headerAccumulator = null;
          nextSate = Segment.BODY;
        } else {
          headerAccumulator.write(currentByte);
        }
      }

      if (currentSegment == Segment.BODY) {
        // if an accumulator doesn't exist, let's create one:
        if (currentPartBodyAccumulator == null) {
          currentPartBodyAccumulator = new TieredOutputStream(DataSize.kibibytes(0), this.bodySizeLimit);
        }

        if (!hasReachedSegmentEnd) {
          currentPartBodyAccumulator.write(currentByte);
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

          // now we need to assess if we're reach the end body or final delimiter, we do that by looking at the next 2 bytes:
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

  private void checkLimits() {
    //TODO: fill limits in
    // limit all stringbuilders
    // limit the amount of bytes to process before starting processing

  }
}
