package org.ophion.jujube.context;

import org.apache.hc.core5.http.ContentType;

import java.nio.file.Path;

public class FileParameter extends PrimitiveParameter {
  private final Path path;
  private final String fileName;

  public FileParameter(String name, Path path, ContentType contentType, String fileName) {
    super(name, null, contentType);
    this.path = path;
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  @Override
  public boolean isText() {
    return false;
  }

  public Path asPath() {
    return path;
  }
}
