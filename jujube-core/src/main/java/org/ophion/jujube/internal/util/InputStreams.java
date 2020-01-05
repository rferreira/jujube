package org.ophion.jujube.internal.util;

import java.io.*;
import java.nio.charset.Charset;

public class InputStreams {
  /**
   * Reads an InputStream fully intro a String. This can be quite inefficient for large streams.
   * Notice: closing the stream must be done by caller.
   *
   * @param ins the InputStream to be read
   * @return a string representing all bytes in the stream.
   */
  public static String read(InputStream ins, Charset charset) {
    StringBuilder contents = new StringBuilder();
    try {
      try (Reader reader = new BufferedReader(new InputStreamReader(ins, charset))) {
        int c;
        while ((c = reader.read()) != -1) {
          contents.append((char) c);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return contents.toString();
  }
}
