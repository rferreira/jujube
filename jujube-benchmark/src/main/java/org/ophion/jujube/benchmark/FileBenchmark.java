package org.ophion.jujube.benchmark;

import org.openjdk.jmh.annotations.*;
import org.ophion.jujube.util.DataSize;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Fork(value = 1, warmups = 1, jvmArgs = "-Xmx64m")
@BenchmarkMode({Mode.Throughput})
public class FileBenchmark {

  @Benchmark
  public void measureWriteSpeedBuffered(BenchmarkState state) throws IOException {
    var ins = new JunkInputStream(DataSize.megabytes(state.payloadInMB).toBytes());
    var ous = new BufferedOutputStream(Files.newOutputStream(state.fd, StandardOpenOption.TRUNCATE_EXISTING));

    int b;
    while ((b = ins.read()) > 0) {
      ous.write(b);
    }
  }

//  @Benchmark
//  public void measureWriteSpeedUnbuffered(BenchmarkState state) throws IOException {
//    var ins = new JunkInputStream(DataSize.megabytes(state.payloadInMB).toBytes());
//    var ous = Files.newOutputStream(state.fd, StandardOpenOption.TRUNCATE_EXISTING);
//    int b;
//    while ((b = ins.read()) > 0) {
//      ous.write(b);
//    }
//  }

  @Benchmark
  public void measureWriteSpeedFileChannel(BenchmarkState state) throws IOException {
    var ins = new JunkInputStream(DataSize.megabytes(state.payloadInMB).toBytes());
    var fileChannel = new RandomAccessFile(state.fd.toFile(), "rw").getChannel();

    byte[] buffer = new byte[8 * 1024];
    while ((ins.read(buffer)) > 0) {
      fileChannel.write(ByteBuffer.wrap(buffer));
    }
    fileChannel.force(true);
  }

  @Benchmark
  public void measureWriteSpeedFileChannelTransferTo(BenchmarkState state) throws IOException {
    var ins = new JunkInputStream(DataSize.megabytes(state.payloadInMB).toBytes());
    var fileChannel = new RandomAccessFile(state.fd.toFile(), "rw").getChannel();

    var chan = Channels.newChannel(ins);
    fileChannel.transferFrom(chan, 0, DataSize.megabytes(state.payloadInMB).toBytes());
    fileChannel.force(true);
  }


  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private final Path fd;
    @Param({"1", "10", "100"})
    public int payloadInMB;

    public BenchmarkState() {
      try {

        fd = Files.createTempFile(null, null);
        System.out.println(">> USING FILE:  " + fd.toString());

      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @TearDown
    public void cleanup() throws IOException {
      Files.deleteIfExists(fd);
    }
  }
}
