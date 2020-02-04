package org.ophion.jujube.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ophion.jujube.internal.util.TieredOutputStream;
import org.ophion.jujube.util.DataSize;

import java.io.IOException;

@Fork(value = 1, warmups = 1, jvmArgs = "-Xmx64m")
@BenchmarkMode({Mode.Throughput})
public class TieredOutputStreamBenchmark {
  @Benchmark
  public void measureWriteSpeed(BenchmarkState state) throws IOException {
    var totalSize = DataSize.megabytes(1);
    try (var out = new TieredOutputStream(DataSize.bytes(state.bufferSizeInBytes), totalSize)) {
      new JunkInputStream(totalSize.toBytes()).transferTo(out);
      out.flush();
    }
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    @Param({"32768", "65536","524288"})
    long bufferSizeInBytes;
  }
}
