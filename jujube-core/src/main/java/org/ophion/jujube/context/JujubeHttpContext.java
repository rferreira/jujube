package org.ophion.jujube.context;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.ophion.jujube.config.JujubeConfig;

import java.util.Optional;
import java.util.stream.Stream;

@Contract(threading = ThreadingBehavior.UNSAFE)
public final class JujubeHttpContext extends HttpCoreContext {
  private static final String PREFIX = "jujube";
  private static final String CONTENT = PREFIX + ".content";
  private static final String CONTENT_TYPE = CONTENT + ".type";
  private final JujubeConfig config;

  public JujubeHttpContext(JujubeConfig config, HttpContext context) {
    super(context);
    this.config = config;
  }

  public Optional<Parameter> getParameter(String name, ParameterSource source) {
    if (source == ParameterSource.HEADER) {
      return Stream.of(getResponse().getHeaders())
        .filter(header -> header.getName().equals(name))
        .map(header -> (Parameter) new PrimitiveParameter(name, header.getValue(), ContentType.TEXT_PLAIN))
        .findFirst();
    }

    return Optional.ofNullable(getAttribute(String.format("%s.%s.%s", PREFIX, source, name), Parameter.class));
  }

  public JujubeConfig getConfig() {
    return config;
  }

  public Optional<HttpEntity> getEntity() {
    return Optional.ofNullable(getAttribute(CONTENT, HttpEntity.class));
  }

  public void setEntity(HttpEntity entity) {
    setAttribute(CONTENT, entity);
    setAttribute(CONTENT_TYPE, ContentType.parse(entity.getContentType()));
  }

  public void setParameter(ParameterSource source, Parameter param) {
    setAttribute(String.format("%s.%s.%s", PREFIX, source, param.name()), param);
  }

  public Optional<ContentType> getEntityContentType() {
    return Optional.ofNullable(getAttribute(CONTENT_TYPE, ContentType.class));
  }

  @Override
  public HttpResponse getResponse() {
    throw new IllegalArgumentException("Response object cannot be retrieved");
  }
}
