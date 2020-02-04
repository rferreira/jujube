package org.ophion.jujube.internal.util;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Limiting, tiered output stream that allows contents to spill over to a file back stream after a certain size.
 * Please note this is very much NOT thread safe.
 * Changes to this class should be accompanied by a benchmark run.
 *
 * @see OutputStream
 */
@Contract(threading = ThreadingBehavior.UNSAFE)
public class TieredOutputStream extends OutputStream implements AutoCloseable {
  private static final Logger LOG = Loggers.build();
  private static final DataSize MINIMAL_BUFFER_SIZE = DataSize.kibibytes(64);
  private final DataSize memoryLimit;
  private boolean isWritingToFile = false;
  private long size;
  private OutputStream fileOutputStream;
  private ByteBuffer buffer;
  private DataSize limit;
  private Path fd;
  private boolean isClosed = false;


  public TieredOutputStream(DataSize limit) {
    this(MINIMAL_BUFFER_SIZE, limit);
  }

  public TieredOutputStream(DataSize memoryLimit, DataSize limit) {
    this(memoryLimit, limit, () -> {
      try {
        return Files.createTempFile("tiered-output-stream-", null);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  public TieredOutputStream(DataSize memoryLimit, DataSize totalLimit, Supplier<Path> tempFileSupplier) {
    this.buffer = ByteBuffer.allocate((int) Math.max(memoryLimit.toBytes(), MINIMAL_BUFFER_SIZE.toBytes()));
    this.limit = totalLimit;
    this.memoryLimit = memoryLimit;

    if (totalLimit.compareTo(memoryLimit) < 0) {
      throw new IllegalArgumentException("total limit cannot be smaller than memory limit");
    }
    this.fd = tempFileSupplier.get();
  }

  @Override
  public void write(int b) throws IOException {
    // pro tip: the more instructions you add here the slower the write-cycle will be,
    // so don't put instructions in here.

    buffer.put((byte) b);

    // should be in-lined by the JIT:
    if (!buffer.hasRemaining()) {
      flush();
    }
  }

  @Override
  public void flush() throws IOException {

    if (isClosed) {
      throw new IllegalStateException("This stream has been closed and cannot be written to!");
    }


    // nothing to flush, return immediately
    if (buffer.position() == 0) {
      return;
    }

    size += buffer.position();

    if (!isWritingToFile && (size >= memoryLimit.toBytes())) {
      isWritingToFile = true;
      LOG.debug("spilling buffer to disk...");
      // no need to double buffer since we're already chunking writes:
      fileOutputStream = Files.newOutputStream(fd, StandardOpenOption.TRUNCATE_EXISTING);
    }

    if (isWritingToFile) {
      fileOutputStream.write(buffer.array(), 0, buffer.position());
      fileOutputStream.flush();

      if (size > limit.toBytes()) {
        throw new IllegalStateException("Max file limit exceeded, cannot process any more bytes");
      }

      buffer.clear();

    }
  }

  /**
   * Returns a input stream suitable for reading this stream's content. If we're not yet writing to a file, we return
   * it directly from memory.
   *
   * @return a stream that allows you to read the entire contents of this stream.
   * @throws IOException if an IO error occurs.
   */
  public InputStream getContentAsStream() throws IOException {
    if (isWritingToFile) {
      // gratis flush to ensure consistency:
      flush();
      return Files.newInputStream(fd, StandardOpenOption.READ);
    } else {
      return new ByteArrayInputStream(buffer.array(), 0, buffer.position());
    }
  }

  /**
   * Returns the entire content of this stream as a String. Please note that this consumes an amount of memory equal
   * to the size of this stream.
   *
   * @param cs the charset to use for the string conversion
   * @return the bytes of this stream converted into a string using the required charset.
   * @throws IOException if there is an IO error.
   */
  public String getContentAsText(Charset cs) throws IOException {
    return InputStreams.read(getContentAsStream(), cs);
  }

  /**
   * Returns the path of the underlying file backing this stream. If this stream has not yet spilled out to its file
   * based tier, we perform a full dump first.
   * For a more efficient way to iterate over this buffer see @{link getContentAsStream}.
   *
   * @return A path to a file containing the contents of this buffer.
   * @throws IOException underlying I/O error.
   */
  public Path getContentsAsPath() throws IOException {
    if (!isWritingToFile) {
      LOG.debug("path requested while contents are only in memory, flushing and returning a temp file");
      //TODO we should try to find a way to flush this without a copy operation:
      Files.write(fd, Arrays.copyOfRange(buffer.array(), 0, buffer.position()));
    }
    // gratis flush to ensure consistency:
    flush();
    return fd;
  }

  /**
   * @return true if this stream has spilled over to writing its contents to a file.
   */
  public boolean isWritingToFile() {
    return isWritingToFile;
  }

  /**
   * Number of bytes written to this stream. Please remember that this size does not include bytes before flushing.
   *
   * @return the number of bytes written to this stream since its creation.
   */
  public long getSize() {
    return size;
  }

  @Override
  public void close() throws IOException {
    if (isClosed) {
      return;
    }
    isClosed = true;
    LOG.debug("deleting {}", fd);
    Files.deleteIfExists(fd);
  }
}
