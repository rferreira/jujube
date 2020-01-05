package org.ophion.jujube.internal.multipart;

import java.nio.file.Path;

public interface MultipartHandler {
  void onTextPart(PartMetadata metadata, String value);

  void onBinaryPart(PartMetadata metadata, Path path);
}
