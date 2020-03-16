package org.ophion.jujube.config;

import java.util.HashMap;
import java.util.Map;

public class StaticAssetsConfig {
  private boolean ifModifiedSinceEnabled = true;
  private boolean lastModifiedEnabled = true;
  private Map<String, String> extensionToMimeMapping;

  public StaticAssetsConfig() {
    extensionToMimeMapping = new HashMap<>();
    extensionToMimeMapping.putAll(MimeMapping.DEFAULT_MAPPING);
  }

  public boolean isLastModifiedEnabled() {
    return lastModifiedEnabled;
  }

  public void setLastModifiedEnabled(boolean lastModifiedEnabled) {
    this.lastModifiedEnabled = lastModifiedEnabled;
  }

  public boolean isIfModifiedSinceEnabled() {
    return ifModifiedSinceEnabled;
  }

  public void setIfModifiedSinceEnabled(boolean ifModifiedSinceEnabled) {
    this.ifModifiedSinceEnabled = ifModifiedSinceEnabled;
  }

  public Map<String, String> getExtensionToMimeMapping() {
    return extensionToMimeMapping;
  }

}
