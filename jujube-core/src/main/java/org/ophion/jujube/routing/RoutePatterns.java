package org.ophion.jujube.routing;

import java.util.Scanner;
import java.util.regex.Pattern;

public class RoutePatterns {
  public static final String SLASH = "/";

  /**
   * Converts higher order Rails-style routes into regex with group name extraction
   * <p>
   * Supports : variables and * globs
   *
   * @param railsRoutePattern the path to parse
   * @return the regex pattern that is required to match this route.
   */
  public static Pattern railsRouteToRegex(String railsRoutePattern) {
    var pattern = new StringBuilder();
    pattern.append("^");

    var scanner = new Scanner(railsRoutePattern).useDelimiter(SLASH);
    while (scanner.hasNext()) {
      var part = scanner.next();
      pattern.append("\\/");

      if (part.startsWith(":")) {
        // (?P<' + parameter + '>' + converter.regex + ')
        pattern.append(String.format("(?<%s>[^/]+)", part.substring(1)));

      } else if (part.startsWith("*")) {
        pattern.append(String.format(".*%s", part.substring(1)));

      } else {
        pattern.append(part.trim());
      }
    }

    // check to see if we need to add back a postfix slash:
    if (railsRoutePattern.endsWith(SLASH)) {
      pattern.append("\\/");
    }

    pattern.append("$");

    return Pattern.compile(pattern.toString());
  }
}
