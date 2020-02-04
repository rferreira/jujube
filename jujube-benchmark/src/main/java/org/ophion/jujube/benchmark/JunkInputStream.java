package org.ophion.jujube.benchmark;

import java.io.IOException;
import java.io.InputStream;

class JunkInputStream extends InputStream {
  private final long sizeInBytes;
  private long position = 0;

  public JunkInputStream(long sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  @Override
  public int read() throws IOException {
    if (position < sizeInBytes) {
      position++;
      return 'X';
    } else {
      return -1;
    }
  }
}
