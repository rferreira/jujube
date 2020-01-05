package org.ophion.jujube.internal.multipart;

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class MultipartChunkDecoderTest {

  @Test
  void shouldDecode() throws IOException {
    var binContents = new byte[]{1, 2, 3, 4, 5};

    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addBinaryBody("file", binContents)
      .addTextBody("hello", "world")
      .build();

    var contents = new ByteArrayOutputStream();
    entity.writeTo(contents);
    var ins = new ByteArrayInputStream(contents.toByteArray());
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
    while (true) {
      byte[] chunk = new byte[8192];
      int bytesRead = ins.read(chunk);
      decoder.decode(chunk, 0, Math.max(bytesRead, 0), bytesRead == -1);
      if (bytesRead == -1) {
        break;
      }
    }
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
    var ins = new ByteArrayInputStream(contents.toByteArray());
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
    }, 80);

    // small chunks:
    while (true) {
      byte[] chunk = new byte[64];
      int bytesRead = ins.read(chunk);
      decoder.decode(chunk, 0, Math.max(bytesRead, 0), bytesRead == -1);
      if (bytesRead == -1) {
        break;
      }
    }


  }

  @Test
  void shouldParseSaved() throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    Map<String, String> expectedValues = Map.of(
      "pi", "3.14159265358979323846264338327950288419716939937510",
      "company", "bob & frank's shoe repair",
      "power", "ꬵо\uD835\uDDCBⲥ\uD835\uDDBE",
      "japanese", "オープンソース",
      "hello", "日食桟橋",
      "upload_file", "filename"
    );
    Map<String, PartMetadata> parts = new HashMap<>();

    var ins = MultipartChunkDecoderTest.class.getClassLoader().getResourceAsStream("multipart/browser-capture-complex-apache-httpcomp.raw");
    if (ins == null) {
      throw new IllegalStateException();
    }

    var decoder = new MultipartChunkDecoder("owr6UQGvVNunA_sx2AsizBtyq_uK-OjsQXrF", new MultipartHandler() {
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
    });

    // small chunks:
    while (true) {
      byte[] chunk = new byte[8192];
      int bytesRead = ins.read(chunk);
      decoder.decode(chunk, 0, Math.max(bytesRead, 0), bytesRead == -1);
      if (bytesRead == -1) {
        break;
      }
    }
    Assertions.assertEquals(expectedValues.size(), parts.size());

  }
}
