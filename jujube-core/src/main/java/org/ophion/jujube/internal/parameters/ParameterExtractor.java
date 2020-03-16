package org.ophion.jujube.internal.parameters;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.ophion.jujube.http.MultipartEntity;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.FileParameter;
import org.ophion.jujube.request.Parameter;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.request.PrimitiveParameter;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterExtractor {
  private static final Logger LOG = Loggers.build();

  public List<Parameter> extract(HttpRequest req, HttpEntity entity) {
    List<Parameter> parameters = new ArrayList<>();

    try {
      URLEncodedUtils.parse(req.getUri(), StandardCharsets.UTF_8)
        .forEach(nvp -> parameters.add(new PrimitiveParameter(ParameterSource.QUERY, nvp.getName(), nvp.getValue(), ContentType.TEXT_PLAIN)));

    } catch (URISyntaxException e) {
      LOG.error("error decoding query string", e);
    }

    // headers:
    parameters.addAll(Stream.of(req.getHeaders())
      .map(header -> (Parameter) new PrimitiveParameter(ParameterSource.HEADER, header.getName(), header.getValue(), ContentType.TEXT_PLAIN))
      .collect(Collectors.toList()));

    if (entity == null) {
      return parameters;
    }

    try {
      final ContentType contentType = ContentType.parseLenient(entity.getContentType());

      if (ContentType.APPLICATION_FORM_URLENCODED.isSameMimeType(contentType)) {
        // TODO: need to find a better way to handle this going forward since this double memory usage
        EntityUtils.parse(entity)
          .forEach(nvp -> parameters.add(new PrimitiveParameter(ParameterSource.FORM, nvp.getName(), nvp.getValue(), ContentType.TEXT_PLAIN)));
      }

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
    } catch (IOException e) {
      LOG.error("error decoding form body", e);
    }
    return parameters;
  }
}
