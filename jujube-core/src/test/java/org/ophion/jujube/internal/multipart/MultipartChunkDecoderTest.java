package org.ophion.jujube.internal.multipart;

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.util.DataSize;
import org.ophion.jujube.util.RepeatingInputStream;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class MultipartChunkDecoderTest {
  private static final Logger LOG = Loggers.build();

  @Test
  void shouldDecode() throws IOException {
    var binContents = new byte[]{1, 2, 3, 4, 5};

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", binContents)
      .addTextBody("hello", "world")
      .build();

    var contentType = ContentType.parse(entity.getContentType());
    var decoder = new MultipartChunkDecoder(contentType.getParameter("boundary"), new MultipartHandler() {
      @Override
      public void onTextPart(PartMetadata metadata, String value) {
        Assertions.assertEquals("hello", metadata.getName());
        Assertions.assertEquals("world", value);
        Assertions.assertEquals(ContentType.TEXT_PLAIN.toString(), metadata.getContentType().toString());
      }

      @Override
      public void onBinaryPart(PartMetadata metadata, Path contents) {
        Assertions.assertEquals("file", metadata.getName());
        Assertions.assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(), metadata.getContentType().toString());
        try {
          Assertions.assertArrayEquals(binContents, Files.readAllBytes(contents));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    // using some small chunks:
    ByteBuffer buffer = ByteBuffer.allocate(8192);
    entity.writeTo(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        buffer.put((byte) b);
        if (!buffer.hasRemaining()) {
          decoder.decode(buffer.flip(), false);
          buffer.clear();
        }
      }
    });
    decoder.decode(buffer.flip(), true);
  }

  @Test
  @Disabled("performance triage test")
  void shouldBeFast() throws Exception {

    var totalSize = DataSize.gigabytes(1);
    var boundary = "11111111111111111111111";

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", new RepeatingInputStream(totalSize.toBytes()))
      .addTextBody("hello", "world")
      .setBoundary(boundary)
      .build();

    Instant start = Instant.now();

    var decoderConfig = new MultipartChunkDecoderConfig();
    decoderConfig.setBodySizeLimit(totalSize);
    decoderConfig.setBufferSize(DataSize.kibibytes(32));

    var decoder = new MultipartChunkDecoder(boundary, new MultipartHandler() {
      @Override
      public void onTextPart(PartMetadata metadata, String value) {
        LOG.info("part received {}", metadata);
      }

      @Override
      public void onBinaryPart(PartMetadata metadata, Path contents) {
        LOG.info("part received {}", metadata);
      }
    }, decoderConfig);

    try (decoder) {
      ByteBuffer buffer = ByteBuffer.allocate((int) DataSize.kibibytes(8).toBytes());

      entity.writeTo(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          buffer.put((byte) b);
          if (!buffer.hasRemaining()) {
            decoder.decode(buffer.flip(), false);
            buffer.clear();
          }
        }
      });
      decoder.decode(buffer.flip(), true);
    }

    var duration = Duration.between(start, Instant.now());
    LOG.warn("throughput of {} MB/s", totalSize.toMegabytes() / duration.toSeconds());
  }

  @Test
  void shouldDecodeSmallChunks() throws IOException {
    var binContents = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", binContents)
      .addTextBody("hello", "world")
      .build();

    var contents = new ByteArrayOutputStream();
    entity.writeTo(contents);
    var contentType = ContentType.parse(entity.getContentType());
    var config = new MultipartChunkDecoderConfig();
    config.setBufferSize(DataSize.bytes(80));


    try (var decoder = new MultipartChunkDecoder(contentType.getParameter("boundary"), new MultipartHandler() {
      @Override
      public void onTextPart(PartMetadata metadata, String value) {
        Assertions.assertEquals("hello", metadata.getName());
        Assertions.assertEquals("world", value);
        Assertions.assertEquals(ContentType.TEXT_PLAIN.toString(), metadata.getContentType().toString());
      }

      @Override
      public void onBinaryPart(PartMetadata metadata, Path contents) {
        Assertions.assertEquals("file", metadata.getName());
        Assertions.assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(), metadata.getContentType().toString());
        try {
          Assertions.assertArrayEquals(binContents, Files.readAllBytes(contents));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }, config)) {
      ByteBuffer buffer = ByteBuffer.allocate(64);
      entity.writeTo(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          buffer.put((byte) b);
          if (!buffer.hasRemaining()) {
            decoder.decode(buffer.flip(), false);
            buffer.clear();
          }
        }
      });
      decoder.decode(buffer.flip(), true);

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void shouldParseSaved() throws IOException {
    Map<String, String> expectedValues = Map.of(
      "pi", "3.14159265358979323846264338327950288419716939937510",
      "company", "bob & frank's shoe repair",
      "power", "ꬵо\uD835\uDDCBⲥ\uD835\uDDBE",
      "japanese", "オープンソース",
      "hello", "日食桟橋",
      "upload_file", "filename"
    );
    Map<String, PartMetadata> parts = new HashMap<>();
    var handler = new MultipartHandler() {
      @Override
      public void onTextPart(PartMetadata metadata, String value) {
        parts.put(metadata.getName(), metadata);
        Assertions.assertEquals(expectedValues.get(metadata.getName()), value);
      }

      @Override
      public void onBinaryPart(PartMetadata metadata, Path contents) {
        parts.put(metadata.getName(), metadata);

        if (metadata.getContentType().isSameMimeType(ContentType.APPLICATION_OCTET_STREAM)) {
          return;
        }

        try {
          Charset charset = metadata.getContentType().getCharset();
          if (charset == null) {
            charset = Charset.defaultCharset();
          }
          Assertions.assertEquals(expectedValues.get(metadata.getName()), Files.readString(contents, charset));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    };

    try (var decoder = new MultipartChunkDecoder("owr6UQGvVNunA_sx2AsizBtyq_uK-OjsQXrF", handler)) {

      var ins = MultipartChunkDecoderTest.class.getClassLoader().getResourceAsStream("multipart/browser-capture-complex-apache-httpcomp.raw");
      if (ins == null) {
        throw new IllegalStateException();
      }

      ByteBuffer buffer = ByteBuffer.allocate(8192);
      ins.transferTo(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          buffer.put((byte) b);
          if (!buffer.hasRemaining()) {
            decoder.decode(buffer.flip(), false);
            buffer.clear();
          }
        }
      });
      decoder.decode(buffer.flip(), true);
      Assertions.assertEquals(expectedValues.size(), parts.size());

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }


  }
}
