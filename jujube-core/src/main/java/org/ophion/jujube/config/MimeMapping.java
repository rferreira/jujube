package org.ophion.jujube.config;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

class MimeMapping {
  public static Map<String, String> DEFAULT_MAPPING;

  static {
    try {
      var props = new Properties();
      props.load(MimeMapping.class.getResourceAsStream("/mime.properties"));
      DEFAULT_MAPPING = props.entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
