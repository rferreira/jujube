package org.ophion.jujube.internal.multipart;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Based upon Jetty's multipart tests.
 *
 * @see <a href="https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-http/src/test/java/org/eclipse/jetty/http/MultiPartCaptureTest.java"></a>
 */
public class MultipartChunkDecoderConformityTest {
  private static final Logger LOG = Loggers.build();
  private final List<Path> toDelete = new ArrayList<>();

  public static Stream<Arguments> data() {
    return Stream.of(
      // == Arbitrary / Non-Standard Examples ==

      "multipart-uppercase",
      // "multipart-base64",  // base64 transfer encoding deprecated
      // "multipart-base64-long", // base64 transfer encoding deprecated

      // == Capture of raw request body contents from Apache HttpClient 4.5.5 ==

      "browser-capture-company-urlencoded-apache-httpcomp",
      "browser-capture-complex-apache-httpcomp",
      "browser-capture-duplicate-names-apache-httpcomp",
      "browser-capture-encoding-mess-apache-httpcomp",
      "browser-capture-nested-apache-httpcomp",
      "browser-capture-nested-binary-apache-httpcomp",
      "browser-capture-number-only2-apache-httpcomp",
      "browser-capture-number-only-apache-httpcomp",
      "browser-capture-sjis-apache-httpcomp",
      "browser-capture-strange-quoting-apache-httpcomp",
      "browser-capture-text-files-apache-httpcomp",
      "browser-capture-unicode-names-apache-httpcomp",
      "browser-capture-zalgo-text-plain-apache-httpcomp",

      // == Capture of raw request body contents from Eclipse Jetty Http Client 9.4.9 ==

      "browser-capture-complex-jetty-client",
      "browser-capture-duplicate-names-jetty-client",
      "browser-capture-encoding-mess-jetty-client",
      "browser-capture-nested-jetty-client",
      "browser-capture-number-only-jetty-client",
      "browser-capture-sjis-jetty-client",
      "browser-capture-text-files-jetty-client",
      "browser-capture-unicode-names-jetty-client",
      "browser-capture-whitespace-only-jetty-client",

      // == Capture of raw request body contents from various browsers ==

      // simple form - 2 fields
      "browser-capture-form1-android-chrome",
      "browser-capture-form1-android-firefox",
      "browser-capture-form1-chrome",
      "browser-capture-form1-edge",
      "browser-capture-form1-firefox",
      "browser-capture-form1-ios-safari",
      "browser-capture-form1-msie",
      "browser-capture-form1-osx-safari",

      // form submitted as shift-jis
      "browser-capture-sjis-form-edge",
      "browser-capture-sjis-form-msie",
      // TODO: these might be addressable via Issue #2398
      // "browser-capture-sjis-form-android-chrome", // contains html encoded character and unspecified charset defaults to utf-8
      // "browser-capture-sjis-form-android-firefox", // contains html encoded character and unspecified charset defaults to utf-8
      // "browser-capture-sjis-form-chrome", // contains html encoded character and unspecified charset defaults to utf-8
      // "browser-capture-sjis-form-firefox", // contains html encoded character and unspecified charset defaults to utf-8
      // "browser-capture-sjis-form-ios-safari", // contains html encoded character and unspecified charset defaults to utf-8
      // "browser-capture-sjis-form-safari", // contains html encoded character and unspecified charset defaults to utf-8

      // form submitted as shift-jis (with HTML5 specific hidden _charset_ field)
      "browser-capture-sjis-charset-form-android-chrome", // contains html encoded character
      "browser-capture-sjis-charset-form-android-firefox", // contains html encoded character
      "browser-capture-sjis-charset-form-chrome", // contains html encoded character
      "browser-capture-sjis-charset-form-edge",
      "browser-capture-sjis-charset-form-firefox", // contains html encoded character
      "browser-capture-sjis-charset-form-ios-safari", // contains html encoded character
      "browser-capture-sjis-charset-form-msie",
      "browser-capture-sjis-charset-form-safari", // contains html encoded character

      // form submitted with simple file upload
      "browser-capture-form-fileupload-android-chrome",
      "browser-capture-form-fileupload-android-firefox",
      "browser-capture-form-fileupload-chrome",
      "browser-capture-form-fileupload-edge",
      "browser-capture-form-fileupload-firefox",
      "browser-capture-form-fileupload-ios-safari",
      "browser-capture-form-fileupload-msie",
      "browser-capture-form-fileupload-safari",

      // form submitted with 2 files (1 binary, 1 text) and 2 text fields
      "browser-capture-form-fileupload-alt-chrome",
      "browser-capture-form-fileupload-alt-edge",
      "browser-capture-form-fileupload-alt-firefox",
      "browser-capture-form-fileupload-alt-msie",
      "browser-capture-form-fileupload-alt-safari"
    ).map(Arguments::of);
  }

  @AfterEach
  void tearDown() throws IOException {
    for (Path path : toDelete) {
      Files.deleteIfExists(path);
    }
  }

  @ParameterizedTest()
  @MethodSource("data")
  @SuppressWarnings("ConstantConditions")
  public void shouldPassConformityTest(String rawPrefix) throws Exception {
    LOG.debug("processing: {}", "multipart/" + rawPrefix + ".raw");
    Path multipartRawFile = Path.of(this.getClass().getClassLoader().getResource("multipart/" + rawPrefix + ".raw").toURI());
    Path expectationPath = Path.of(this.getClass().getClassLoader().getResource("multipart/" + rawPrefix + ".expected.txt").toURI());
    MultipartExpectations multipartExpectations = new MultipartExpectations(expectationPath);

    Map<PartMetadata, Object> parts = new HashMap<>();
    var boundary = ContentType.parse(multipartExpectations.contentType).getParameter("boundary");

    try (InputStream ins = Files.newInputStream(multipartRawFile)) {
      MultipartChunkDecoder decoder = new MultipartChunkDecoder(boundary, new MultipartHandler() {
        @Override
        public void onTextPart(PartMetadata metadata, String value) {
          parts.put(metadata, value);
        }

        @Override
        public void onBinaryPart(PartMetadata metadata, Path path) {
          parts.put(metadata, path);
          toDelete.add(path);
        }
      });
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

      multipartExpectations.checkParts(parts, s -> parts.keySet().stream()
        .filter(part -> part.getName().equals(s)).findFirst()
        .orElseThrow(() -> new IllegalStateException("could not find part named:" + s)));
    }
  }

  @Test
  @Disabled
  void triage() throws Exception {
    shouldPassConformityTest("multipart-uppercase");
  }

  public static class NameValue {
    public String name;
    public String value;
  }

  public static class MultipartExpectations {
    public final String contentType;
    public final int partCount;
    public final List<NameValue> partFilenames = new ArrayList<>();
    public final List<NameValue> partSha1sums = new ArrayList<>();
    public final List<NameValue> partContainsContents = new ArrayList<>();

    public MultipartExpectations(Path expectationsPath) throws IOException {
      String parsedContentType = null;
      String parsedPartCount = "-1";

      try (BufferedReader reader = Files.newBufferedReader(expectationsPath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.length() == 0 || line.startsWith("#")) {
            // skip blanks and comments
            continue;
          }

          String[] split = line.split("\\|");
          switch (split[0]) {
            case "Request-Header":
              if (split[1].equalsIgnoreCase("Content-Type")) {
                parsedContentType = split[2];
              }
              break;
            case "Content-Type":
              parsedContentType = split[1];
              break;
            case "Parts-Count":
              parsedPartCount = split[1];
              break;
            case "Part-ContainsContents": {
              NameValue pair = new NameValue();
              pair.name = split[1];
              pair.value = split[2];
              partContainsContents.add(pair);
              break;
            }
            case "Part-Filename": {
              NameValue pair = new NameValue();
              pair.name = split[1];
              pair.value = split[2];
              partFilenames.add(pair);
              break;
            }
            case "Part-Sha1sum": {
              NameValue pair = new NameValue();
              pair.name = split[1];
              pair.value = split[2];
              partSha1sums.add(pair);
              break;
            }
            default:
              throw new IOException("Bad Line in " + expectationsPath + ": " + line);
          }
        }
      }

      Objects.requireNonNull(parsedContentType, "Missing required 'Content-Type' declaration: " + expectationsPath);
      this.contentType = parsedContentType;
      this.partCount = Integer.parseInt(parsedPartCount);
    }

    private void checkParts(Map<PartMetadata, Object> parts, Function<String, PartMetadata> getPart) throws Exception {
      // Evaluate Count
      if (partCount >= 0) {
        Assertions.assertEquals(partCount, parts.size());
      }

      // Evaluate expected Contents
      for (NameValue expected : partContainsContents) {
        PartMetadata part = getPart.apply(expected.name);
        Assertions.assertEquals(expected.name, part.getName());
        if (part.isText()) {
          Assertions.assertTrue(((String) parts.get(part)).contains(expected.value));
        } else {
          Charset charset = part.getContentType().getCharset();
          if (charset == null) {
            charset = Charset.defaultCharset();
          }
          Assertions.assertTrue((Files.readString((Path) parts.get(part), charset)).contains(expected.value));
        }
      }
//
      // Evaluate expected filenames
      for (NameValue expected : partFilenames) {
        PartMetadata part = getPart.apply(expected.name);
        Assertions.assertEquals(expected.name, part.getName());
        Assertions.assertEquals(expected.value, part.getFilename());
      }

      // Evaluate expected contents checksums
      for (NameValue expected : partSha1sums) {
        PartMetadata part = getPart.apply(expected.name);
        Assertions.assertEquals(expected.name, part.getName());

        String sha1Hex;

        if (part.isText()) {
          sha1Hex = DigestUtils.sha1Hex((String) parts.get(part));
        } else {
          try (InputStream ins = Files.newInputStream((Path) parts.get(part))) {
            sha1Hex = DigestUtils.sha1Hex(ins);
          }

        }

        Assertions.assertEquals(expected.value.toLowerCase(), sha1Hex);
      }
    }
  }
}
