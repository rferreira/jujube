package org.ophion.jujube.request;

import org.apache.hc.core5.http.ContentType;

public class PrimitiveParameter implements Parameter {
  private final String name;
  private final String value;
  private final ContentType contentType;
  private final ParameterSource source;

  public PrimitiveParameter(ParameterSource source, String name, String value, ContentType contentType) {
    this.name = name;
    this.value = value;
    this.contentType = contentType;
    this.source = source;
  }

  @Override
  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public boolean isText() {
    return true;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String asText() {
    return value;
  }

  public int asInteger() {
    return Integer.parseInt(value);
  }

  public float asFloat() {
    return Float.parseFloat(value);
  }

  @Override
  public long asLong() {
    return Long.parseLong(value);
  }

  @Override
  public ParameterSource source() {
    return source;
  }
}
