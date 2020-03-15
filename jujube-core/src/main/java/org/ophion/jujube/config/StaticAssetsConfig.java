package org.ophion.jujube.config;

import java.util.HashMap;
import java.util.Map;

public class StaticAssetsConfig {
  private boolean ifModifiedSinceEnabled;
  private boolean isEtagEnabled;
  private Map<String, String> extensionToMimeMapping;

  public StaticAssetsConfig() {

    extensionToMimeMapping = new HashMap<>();
    extensionToMimeMapping.putAll(MimeMapping.DEFAULT_MAPPING);
  }

  public boolean isEtagEnabled() {
    return isEtagEnabled;
  }

  public void setEtagEnabled(boolean etagEnabled) {
    isEtagEnabled = etagEnabled;
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
