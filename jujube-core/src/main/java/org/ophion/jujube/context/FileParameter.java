package org.ophion.jujube.context;

import org.apache.hc.core5.http.ContentType;

import java.nio.file.Path;

public class FileParameter implements Parameter
{
  private final Path path;
  private final ContentType contentType;
  private final String fileName;
  private final String name;

  public FileParameter(String name, Path path, ContentType contentType, String fileName) {
    this.path = path;
    this.contentType = contentType;
    this.name = name;
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  @Override
  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public boolean isText() {
    return false;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String value() {
    return path.toString();
  }

  public Path asPath() {
    return path;
  }
}
