package org.ophion.jujube.request;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.routing.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PathParameterExtractor implements ParameterExtractor {
  private static Pattern NAMED_GROUP_EXTRACT_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  private List<String> extractNamedGroups(Pattern pattern) {
    var entries = new ArrayList<String>();

    // extracting named groups:
    // https://bugs.openjdk.java.net/browse/JDK-7032377
    var matcher = NAMED_GROUP_EXTRACT_PATTERN.matcher(pattern.pattern());
    while (matcher.find()) {
      entries.add(matcher.group(1));
    }
    return entries;
  }

  @Override
  public List<Parameter> extract(HttpRequest req, HttpEntity entity, HttpContext ctx) {
    var params = new ArrayList<Parameter>();
    Route route = (Route) ctx.getAttribute("jujube.route");

    var namedGroups = extractNamedGroups(route.getPattern());

    if (namedGroups.size() > 0) {
      var matcher = route.getPattern().matcher(req.getPath());
      if (matcher.matches()) {
        for (String name : namedGroups) {
          params.add(new PrimitiveParameter(ParameterSource.PATH, name, matcher.group(name), ContentType.TEXT_PLAIN));
        }
      }
    }
    return params;
  }
}
