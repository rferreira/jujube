package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class MultipartEntity extends AbstractHttpEntity {
  private final List<Part> parts;

  public MultipartEntity(List<Part> parts, ContentType contentType, String contentEncoding) {
    super(contentType, contentEncoding);
    this.parts = parts;
  }

  @Override
  public InputStream getContent() throws IOException, UnsupportedOperationException {
    throw new UnsupportedOperationException("In a multi part entity you must acquire streams from the underlying entities");
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public long getContentLength() {
    return 0;
  }

  public List<Part> getParts() {
    return parts;
  }

  public static class Part implements NameValuePair {
    private final String name;
    private final String value;
    private final String filename;
    private final ContentType contentType;
    private final Map<String, String> headers;

    public Part(String name, String value, String filename, ContentType contentType, Map<String, String> headers) {
      this.name = name;
      this.value = value;
      this.filename = filename;
      this.contentType = contentType;
      this.headers = headers;
    }

    public boolean isText() {
      return contentType.toString().toLowerCase().startsWith("text/");
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public String getFilename() {
      return filename;
    }

    public ContentType getContentType() {
      return contentType;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }
  }
}
