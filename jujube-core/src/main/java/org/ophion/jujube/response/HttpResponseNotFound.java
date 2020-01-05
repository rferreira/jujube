package org.ophion.jujube.response;

import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog;

import java.util.Locale;

public class HttpResponseNotFound extends DefaultHttpResponse {
  public HttpResponseNotFound() {
    super(HttpStatus.SC_NOT_FOUND);
    setContent(EnglishReasonPhraseCatalog.INSTANCE.getReason(getCode(), Locale.getDefault()));
  }
}
