package org.ophion.jujube.response;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog;

import java.util.Locale;

public class HttpResponse extends DefaultHttpResponse {
  public HttpResponse() {
    super(HttpStatus.SC_NO_CONTENT);
  }

  public HttpResponse(String content) {
    this(content, ContentType.TEXT_PLAIN);
  }

  public HttpResponse(String content, ContentType contentType) {
    super(HttpStatus.SC_OK);
    setContentType(contentType);
    setContent(content);
  }

  public HttpResponse(int code) {
    super(code);
    setContent(EnglishReasonPhraseCatalog.INSTANCE.getReason(getCode(), Locale.getDefault()));
  }
}
