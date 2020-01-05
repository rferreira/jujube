package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;

import java.util.Map;

public class Parameter implements NameValuePair {
  private final String name;
  private final String value;
  private final ContentType contentType;
  private final Map<String, String> headers;
  private String fileName;

  public Parameter(String name, String value, ContentType contentType, Map<String, String> headers) {
    this.name = name;
    this.value = value;
    this.contentType = contentType;
    this.headers = headers;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getValue() {
    return value;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  public ContentType getContentType() {
    return contentType;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public enum Source {
    PATH, QUERY, HEADER, FORM;
  }
}
