package org.ophion.jujube.response;

import org.apache.hc.core5.http.ContentType;

public interface JujubeHttpResponse extends org.apache.hc.core5.http.HttpResponse {
  ContentType getContentType();

  void setContentType(ContentType contentType);

  Object getContent();

  void setContent(Object content);
}
