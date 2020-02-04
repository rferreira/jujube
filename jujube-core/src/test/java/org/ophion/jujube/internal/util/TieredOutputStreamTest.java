package org.ophion.jujube.internal.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RepeatingInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

class TieredOutputStreamTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldTierData() throws IOException {
    try (var out = new TieredOutputStream(DataSize.kilobytes(500))) {
      new RepeatingInputStream(DataSize.kibibytes(100).toBytes()).transferTo(out);
      out.flush();
      Assertions.assertEquals(DataSize.kibibytes(100).toBytes(), out.getSize());
      Assertions.assertTrue(out.isWritingToFile());
    }
  }

  @Test
  @Disabled("troubleshooting test, shouldn't be run in a regular CI build")
  void shouldBeFast() throws IOException {
    Instant start = Instant.now();
    var totalSize = DataSize.gigabytes(1);
    try (var out = new TieredOutputStream(totalSize)) {
      var ins = new RepeatingInputStream(totalSize.toBytes());
      int b;
      while ((b = ins.read()) > 0) {
        out.write(b);
      }
    }
    var duration = Duration.between(start, Instant.now());
    LOG.warn("throughput of {} MB/s", totalSize.toMegabytes() / duration.toSeconds());
  }

  @Test
  void shouldLimitBuffer() throws IOException {
    try (var out = new TieredOutputStream(DataSize.kilobytes(1), DataSize.kilobytes(42))) {
      Assertions.assertThrows(IllegalStateException.class, () -> {
        new RepeatingInputStream(DataSize.kilobytes(43).toBytes()).transferTo(out);
        out.flush();
      });
    }

    try (var out = new TieredOutputStream(DataSize.kilobytes(1), DataSize.kilobytes(1))) {
      Assertions.assertThrows(IllegalStateException.class, () -> {
        new RepeatingInputStream(DataSize.kilobytes(43).toBytes()).transferTo(out);
        out.flush();
      });
    }
  }

  @Test
  void shouldLimitImpossibleLimits() throws IOException {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      try (var out = new TieredOutputStream(DataSize.kilobytes(10), DataSize.kilobytes(1))) {
        new RepeatingInputStream(DataSize.kilobytes(43).toBytes()).transferTo(out);
        out.flush();
      }
    });

  }

  @Test
  void shouldNotAllowWriteAfterClose() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
      try (var out = new TieredOutputStream(DataSize.kilobytes(1_000))) {
        out.close();
        new RepeatingInputStream(DataSize.kilobytes(1).toBytes()).transferTo(out);
        out.flush();
      }
    });

  }
}
