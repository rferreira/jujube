package org.ophion.jujube.internal.multipart;

import java.nio.file.Path;

public class NullMultipartHandler implements MultipartHandler {

  @Override
  public void onTextPart(PartMetadata metadata, String value) {

  }

  @Override
  public void onBinaryPart(PartMetadata metadata, Path contents) {

  }
}
