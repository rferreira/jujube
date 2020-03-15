package org.ophion.jujube.response;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;

public class JujubeResponse extends BasicHttpResponse {
  private ContentType contentType = ContentType.TEXT_PLAIN;
  private Object content;

  public JujubeResponse() {
    this(HttpStatus.SC_NO_CONTENT);
  }

  public JujubeResponse(Object content) {
    this(HttpStatus.SC_OK);
    this.content = content;
  }

  public JujubeResponse(int code) {
    super(code);
  }

  public JujubeResponse(int code, Object content) {
    this(code);
    this.content = content;
  }

  public JujubeResponse(int code, Object content, ContentType contentType) {
    this(code);
    this.contentType = contentType;
    this.content = content;
  }

  public ContentType getContentType() {
    return this.contentType;
  }

  public void setContentType(ContentType contentType) {
    this.contentType = contentType;
  }

  public Object getContent() {
    return content;
  }

  public void setContent(Object content) {
    this.content = content;
  }
}
