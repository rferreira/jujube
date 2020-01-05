package org.ophion.jujube.internal.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.IntStream;

class TieredOutputStreamTest {

  @Test
  void shouldTierData() throws IOException {
    try (var out = new TieredOutputStream(DataSize.bytes(1), DataSize.bytes(3))) {
      var data = new byte[]{1, 2, 3};
      out.write(data);
      Assertions.assertEquals(1, out.getBytesWrittenForTier(TieredOutputStream.Tier.MEMORY));
      Assertions.assertEquals(3, out.getBytesWrittenForTier(TieredOutputStream.Tier.FILE));

      Assertions.assertArrayEquals(data, out.getContentAsStream().readAllBytes());
      Assertions.assertEquals(TieredOutputStream.Tier.FILE, out.getCurrentTier());
    }
  }

  @Test
  void shouldTierData2() throws IOException {
    try (var out = new TieredOutputStream(DataSize.kibibytes(100), DataSize.kibibytes(1_000))) {
      IntStream.rangeClosed(1, (int) DataSize.kibibytes(1_000).toBytes())
        .forEach(i -> {
          try {
            out.write(i);
          } catch (IOException e) {
            throw new IllegalStateException();
          }
        });
      Assertions.assertEquals(DataSize.kibibytes(100).toBytes(), out.getBytesWrittenForTier(TieredOutputStream.Tier.MEMORY));
      Assertions.assertEquals(DataSize.kibibytes(1_000).toBytes(), out.getBytesWrittenForTier(TieredOutputStream.Tier.FILE));
      Assertions.assertEquals(TieredOutputStream.Tier.FILE, out.getCurrentTier());
    }
  }

  @Test
  void shouldAllowForTierUsageControl() throws IOException {
    try (var out = new TieredOutputStream(DataSize.kibibytes(0), DataSize.kibibytes(1000))) {
      IntStream.rangeClosed(1, (int) DataSize.kibibytes(1000).toBytes())
        .forEach(i -> {
          try {
            out.write(i);
          } catch (IOException e) {
            throw new IllegalStateException();
          }
        });
      Assertions.assertEquals(DataSize.kibibytes(0).toBytes(), out.getBytesWrittenForTier(TieredOutputStream.Tier.MEMORY));
      Assertions.assertEquals(DataSize.kibibytes(1000).toBytes(), out.getBytesWrittenForTier(TieredOutputStream.Tier.FILE));
      Assertions.assertEquals(TieredOutputStream.Tier.FILE, out.getCurrentTier());
    }
  }
}
