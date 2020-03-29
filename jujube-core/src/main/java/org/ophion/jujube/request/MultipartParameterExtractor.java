package org.ophion.jujube.request;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.http.MultipartEntity;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipartParameterExtractor implements ParameterExtractor {
  private static final Logger LOG = Loggers.build();

  @Override
  public List<Parameter> extract(HttpRequest req, HttpEntity entity, HttpContext ctx) {

    if (entity == null || entity.getContentType() == null) {
      return Collections.emptyList();
    }

    var parameters = new ArrayList<Parameter>();
    final ContentType contentType = ContentType.parseLenient(entity.getContentType());

    if (ContentType.MULTIPART_FORM_DATA.isSameMimeType(contentType)) {
      var parts = ((MultipartEntity) entity).getParts();
      parts.forEach(p -> {
        if (p.isText()) {
          parameters.add(new PrimitiveParameter(ParameterSource.FORM, p.getName(), p.getValue(), p.getContentType()));
        } else {
          parameters.add(new FileParameter(ParameterSource.FORM, p.getName(), Paths.get(p.getValue()), p.getContentType(), p.getFilename()));
        }
      });
    }
    return parameters;
  }
}
