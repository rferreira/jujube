package org.ophion.jujube;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.ophion.jujube.config.JujubeConfig;

import java.util.Optional;

@Contract(threading = ThreadingBehavior.UNSAFE)
public final class JujubeHttpContext extends HttpCoreContext {
  private static final String PREFIX = "jujube";
  private static final String CONTENT = PREFIX + ".content";
  private static final String CONTENT_TYPE = CONTENT + ".type";

  public JujubeHttpContext(HttpContext context) {
    super(context);
  }

  public Optional<String> getParameter(String name, ParameterSource source) {
    return Optional.ofNullable(getAttribute(String.format("%s.%s.%s", PREFIX, source, name), String.class));
  }

  public JujubeConfig getConfig() {
    return (JujubeConfig) getAttribute(PREFIX + "config");
  }

  public Optional<HttpEntity> getEntity() {
    return Optional.ofNullable(getAttribute(CONTENT, HttpEntity.class));
  }

  public void setEntity(HttpEntity entity) {
    setAttribute(CONTENT, entity);
    setAttribute(CONTENT_TYPE, ContentType.parse(entity.getContentType()));
  }

  public void setParameter(ParameterSource source, String name, String value) {
    setAttribute(String.format("%s.%s.%s", PREFIX, source, name), value);
  }

  public Optional<ContentType> getEntityContentType() {
    return Optional.ofNullable(getAttribute(CONTENT_TYPE, ContentType.class));
  }
}
