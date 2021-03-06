package org.ophion.jujube.request;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.HttpRequestWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Extension of Apache's Http Request {@link HttpRequest} to include helper functionality.
 */
public class JujubeRequest extends HttpRequestWrapper {
  private List<Parameter> parameters;
  private HttpEntity entity;

  public JujubeRequest(HttpRequest message, HttpEntity entity) {
    super(message);
    this.entity = entity;
    parameters = Collections.emptyList();
  }

  public void setEntity(HttpEntity entity) {
    this.entity = entity;
  }

  /**
   * Returns the underlying HTTP entity, if any.
   *
   * @return http entity if it exists.
   */
  public Optional<HttpEntity> getHttpEntity() {
    return Optional.ofNullable(this.entity);
  }

  /**
   * Returns the first parameter found with name and source {@link ParameterSource}.
   * If you would like to pull out parameters with duplicate entries, please see {@see getParameters}.
   *
   * @param name   the name of the parameter.
   * @param source the source of the parameter.
   * @return parameter - if found.
   */
  public Optional<Parameter> getParameter(String name, ParameterSource source) {
    return parameters.stream()
      .filter(p -> p.source() == source)
      .filter(p -> p.name().equals(name))
      .findFirst();
  }

  /**
   * Returns the set of all known parameters
   *
   * @return parameter set.
   */
  public List<Parameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<Parameter> parameters) {
    this.parameters = parameters;
  }
}
