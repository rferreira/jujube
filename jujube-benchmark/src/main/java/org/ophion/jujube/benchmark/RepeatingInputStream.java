package org.ophion.jujube.benchmark;

import java.io.InputStream;

class RepeatingInputStream extends InputStream {
  private final long sizeInBytes;
  private long position = 0;

  public RepeatingInputStream(long sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  @Override
  public int read() {
    if (position < sizeInBytes) {
      position++;
      return 'X';
    } else {
      return -1;
    }
  }
}
