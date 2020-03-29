package org.ophion.jujube.request;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.List;

public interface ParameterExtractor {
  List<Parameter> extract(HttpRequest req, HttpEntity entity, HttpContext ctx);
}
