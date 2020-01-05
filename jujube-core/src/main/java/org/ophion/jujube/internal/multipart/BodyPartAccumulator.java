package org.ophion.jujube.internal.multipart;

import org.apache.hc.core5.http.ContentType;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class BodyPartAccumulator {
  private final ContentType contentType;
  private BufferedOutputStream fileBuffer = null;
  private ByteArrayOutputStream memoryBuffer = null;
  private Path fd = null;
  private boolean isBinary = false;
  private long bytesAccumulated;

  public BodyPartAccumulator(ContentType contentType) throws IOException {
    this.contentType = contentType;
    if (contentType.toString().toLowerCase().startsWith("text/")) {
      memoryBuffer = new ByteArrayOutputStream();
    } else {
      fd = Files.createTempFile("jujube-multipart-", null);
      fileBuffer = new BufferedOutputStream(Files.newOutputStream(fd));
      isBinary = true;
    }
  }

  public void append(byte[] content) throws IOException {
    if (isBinary) {
      fileBuffer.write(content);
    } else {
      memoryBuffer.write(content);
    }
    bytesAccumulated += content.length;
  }

  public void append(byte content) throws IOException {
    if (isBinary) {
      fileBuffer.write(content);
    } else {
      memoryBuffer.write(content);
    }
    bytesAccumulated++;

  }

  public String getText() {
    Charset charset = contentType.getCharset();
    if (charset == null) {
      charset = Charset.defaultCharset();
    }
    return memoryBuffer.toString(charset);
  }

  public Path getFilePath() {
    try {
      fileBuffer.flush();
      fileBuffer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return fd;
  }

  @Override
  public String toString() {
    return "BodyPartAccumulator{" +
      "contentType=" + contentType +
      ", isBinary=" + isBinary +
      '}';
  }

  public long getBytesAccumulated() {
    return bytesAccumulated;
  }
}
