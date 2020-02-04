package org.ophion.jujube.context;

import org.apache.hc.core5.http.ContentType;

public class TextParameter implements Parameter {
  private final String name;
  private final String value;
  private final ContentType contentType;

  public TextParameter(String name, String value, ContentType contentType) {
    this.name = name;
    this.value = value;
    this.contentType = contentType;
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
  public String value() {
    return value;
  }
}
