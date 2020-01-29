package org.ophion.jujube.internal;

import java.io.IOException;

public class PostSizeLimitExceeded extends IOException {
  public PostSizeLimitExceeded(String message) {
    super(message);
  }
}
