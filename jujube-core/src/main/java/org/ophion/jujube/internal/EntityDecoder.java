package org.ophion.jujube.internal;

import org.apache.hc.core5.http.HttpEntity;
import org.ophion.jujube.JujubeHttpContext;

public interface EntityDecoder {
  void decode(JujubeHttpContext context, HttpEntity entity);
}
