package org.ophion.jujube.internal.util;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.util.ByteArrayBuffer;
import org.ophion.jujube.util.DataSize;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Somewhat over-engineered, potentially infinite, OutputStream that operates on multiple backing streams. Let's say you
 * have a byte stream of unknown size, and you would like it to end up on disk if it's greater than 1MB and memory
 * otherwise - this streams allow you to do that.
 * <p>
 * Limits are cumulative since we copy bytes from the current tier to the next tier once we hit the current tier limit.
 *
 * @see OutputStream
 */
@Contract(threading = ThreadingBehavior.UNSAFE)
public class TieredOutputStream extends OutputStream implements AutoCloseable {
  public static final int DEFAULT_BUFFER_SIZE = (int) DataSize.kibibytes(8).toBytes();
  private static final Logger LOG = Loggers.build();
  private final Map<Tier, DataSize> limits;
  private final Map<Tier, TierHandler> handlers;
  private final long[] tierUsageTrackerInBytes;
  private Tier currentTier;

  public TieredOutputStream(DataSize memoryLimit, DataSize diskLimit) {
    this(memoryLimit, diskLimit, () -> {
      try {
        return Files.createTempFile(null, null);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  public TieredOutputStream(DataSize memoryLimit, DataSize diskLimit, Supplier<Path> tempFileSupplier) {
    this.limits = Map.of(Tier.MEMORY, memoryLimit, Tier.FILE, diskLimit);
    this.handlers = new HashMap<>();

    handlers.put(Tier.MEMORY, new TierHandler() {
      private ByteArrayBuffer buffer;

      @Override
      public void close() {
        LOG.debug("closing {} buffer", Tier.MEMORY);
        buffer = null;
      }

      @Override
      public void consume(int b) {
        if (buffer == null) {
          LOG.debug("creating stream for the tier: {}", Tier.MEMORY);
          buffer = new ByteArrayBuffer(DEFAULT_BUFFER_SIZE);
        }
        buffer.append(b);
      }

      @Override
      public InputStream contents() {
        return new ByteArrayInputStream(buffer.array(), 0, buffer.length());
      }

      @Override
      public boolean isFileBacked() {
        return false;
      }

      @Override
      public Path getPath() {
        return null;
      }
    });

    handlers.put(Tier.FILE, new TierHandler() {
      private Path fileBufferPath = tempFileSupplier.get();
      private BufferedOutputStream buffer;

      @Override
      public void close() throws Exception {
        buffer.flush();
        buffer.close();
        buffer = null;
        LOG.debug("deleting buffer {}", fileBufferPath);
        Files.deleteIfExists(fileBufferPath);
      }

      @Override
      public void consume(int b) {
        try {
          if (buffer == null) {
            LOG.debug("creating stream for the tier: {} with path: {}", Tier.FILE, fileBufferPath);
            buffer = new BufferedOutputStream(Files.newOutputStream(fileBufferPath), DEFAULT_BUFFER_SIZE);
          }
          buffer.write(b);

        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }

      @Override
      public InputStream contents() {
        try {
          buffer.flush();
          return Files.newInputStream(fileBufferPath);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }

      @Override
      public boolean isFileBacked() {
        return true;
      }

      @Override
      public Path getPath() {
        try {
          buffer.flush();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        return fileBufferPath;
      }
    });

    // always start from the lowest tier:
    this.currentTier = Tier.values()[0];
    this.tierUsageTrackerInBytes = new long[Tier.values().length];
  }

  @Override
  public void write(int b) throws IOException {
    var limit = limits.get(currentTier);

    if (tierUsageTrackerInBytes[currentTier.ordinal()] == limit.toBytes()) {
      LOG.debug("limit of {} reached for tier {}", limit, currentTier);
      if (Tier.values().length - 1 == currentTier.ordinal()) {
        throw new IllegalStateException("Storage limit reached for all tiers, cannot proceed");
      }

      // we bumped up the tier, so we need to migrate the data
      Tier nextTier = Tier.values()[currentTier.ordinal() + 1];
      var currentTierConsumer = handlers.get(currentTier);

      // move contents to the next tier if needed:
      if (tierUsageTrackerInBytes[currentTier.ordinal()] > 0) {
        var nextTierConsumer = handlers.get(nextTier);

        // TODO: maybe we could aggregate (and not copy) the data
        currentTierConsumer.contents().transferTo(new OutputStream() {
          @Override
          public void write(int b) {
            nextTierConsumer.consume(b);
            tierUsageTrackerInBytes[nextTier.ordinal()]++;
          }
        });
      }

      // always make sure to close the previous tier os out:
      try {
        currentTierConsumer.close();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }

      // flipping tier:
      currentTier = nextTier;
    }

    handlers.get(currentTier).consume(b);
    tierUsageTrackerInBytes[currentTier.ordinal()]++;
  }

  public InputStream getContentAsStream() {
    return handlers.get(currentTier).contents();
  }

  public String getContentAsText(Charset cs) {
    return InputStreams.read(getContentAsStream(), cs);
  }

  public Path getContentsAsPath() {
    var consumer = handlers.get(currentTier);
    if (consumer.isFileBacked()) {
      return consumer.getPath();
    }

    throw new IllegalStateException("File path cannot be retrieved for non-file backed tier, current tier is:" + currentTier);
  }

  @Override
  public void close() throws IOException {
    try {
      handlers.get(currentTier).close();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    super.close();
  }

  long getBytesWrittenForTier(Tier tier) {
    return tierUsageTrackerInBytes[tier.ordinal()];
  }

  Tier getCurrentTier() {
    return currentTier;
  }

  /**
   * Buffer tiers in order or priority (lowest to the highest ordinal)
   */
  enum Tier {
    MEMORY, FILE
  }

  private interface TierHandler extends AutoCloseable {
    void consume(int b);

    InputStream contents();

    boolean isFileBacked();

    Path getPath();
  }

}
