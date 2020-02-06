package org.ophion.jujube.context;

import org.apache.hc.core5.http.ContentType;

public interface Parameter {
  ContentType getContentType();

  boolean isText();

  String name();

  String asText();

  int asInteger();

  float asFloat();

  long asLong();
}
