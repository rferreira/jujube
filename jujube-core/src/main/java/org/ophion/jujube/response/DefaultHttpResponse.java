package org.ophion.jujube.response;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicHttpResponse;

public class DefaultHttpResponse extends BasicHttpResponse implements JujubeHttpResponse {
  private ContentType contentType = ContentType.TEXT_PLAIN;
  private Object content;

  public DefaultHttpResponse(int code) {
    super(code);
  }

  @Override
  public ContentType getContentType() {
    return this.contentType;
  }

  @Override
  public void setContentType(ContentType contentType) {
    this.contentType = contentType;
  }

  @Override
  public Object getContent() {
    return content;
  }

  @Override
  public void setContent(Object content) {
    this.content = content;
  }
}
