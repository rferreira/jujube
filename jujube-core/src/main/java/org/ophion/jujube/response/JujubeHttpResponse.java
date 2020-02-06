package org.ophion.jujube.response;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;

public class JujubeHttpResponse extends BasicHttpResponse {
  private ContentType contentType = ContentType.TEXT_PLAIN;
  private Object content;

  public JujubeHttpResponse(int code) {
    super(code);
  }

  public JujubeHttpResponse() {
    super(HttpStatus.SC_NO_CONTENT);
  }


  public JujubeHttpResponse(String content) {
    super(HttpStatus.SC_OK);
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
