package org.ophion.jujube.util;

import java.io.InputStream;

public class RepeatingInputStream extends InputStream {
  private static final byte CHARACTER = 'X';
  private final long sizeInBytes;
  private long position = 0;

  public RepeatingInputStream(long sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  @Override
  public int read() {
    if (position < sizeInBytes) {
      position++;
      return CHARACTER;
    } else {
      return -1;
    }
  }

}
