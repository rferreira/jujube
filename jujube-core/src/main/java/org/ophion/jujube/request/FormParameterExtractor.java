package org.ophion.jujube.request;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormParameterExtractor implements ParameterExtractor {
  private static final Logger LOG = Loggers.build();

  @Override
  public List<Parameter> extract(HttpRequest req, HttpEntity entity, HttpContext ctx) {

    if (entity == null || entity.getContentType() == null) {
      return Collections.emptyList();
    }

    var parameters = new ArrayList<Parameter>();

    try {
      final ContentType contentType = ContentType.parseLenient(entity.getContentType());

      if (ContentType.APPLICATION_FORM_URLENCODED.isSameMimeType(contentType)) {
        // TODO: need to find a better way to handle this going forward since this double memory usage
        EntityUtils.parse(entity)
          .forEach(nvp -> parameters.add(new PrimitiveParameter(ParameterSource.FORM, nvp.getName(), nvp.getValue(), ContentType.TEXT_PLAIN)));
      }

    } catch (IOException e) {
      LOG.error("error decoding form body", e);
    }
    return parameters;
  }
}
