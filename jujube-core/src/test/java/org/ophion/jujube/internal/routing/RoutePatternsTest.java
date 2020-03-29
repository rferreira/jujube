package org.ophion.jujube.internal.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.routing.RoutePatterns;

class RoutePatternsTest {

  @Test
  void shouldConvertRailsRoutes() {
    var pattern = RoutePatterns.railsRouteToRegex("/puppies/:id");
    var m = pattern.matcher("/puppies/1");
    Assertions.assertTrue(m.matches());
    Assertions.assertEquals("1", m.group("id"));
  }

  @Test
  void shouldConvertRailsRoutes2() {
    var pattern = RoutePatterns.railsRouteToRegex("/:id/");
    var m = pattern.matcher("/identifier-123/");
    Assertions.assertTrue(m.matches());
    Assertions.assertEquals("identifier-123", m.group("id"));
  }

  @Test
  void shouldConvertRailsRoutes3() {
    var pattern = RoutePatterns.railsRouteToRegex("/accounts/:id/assets/:assetId");
    var m = pattern.matcher("/accounts/123/assets/456");
    Assertions.assertTrue(m.matches());
    Assertions.assertEquals("123", m.group("id"));
    Assertions.assertEquals("456", m.group("assetId"));
  }

  @Test
  void shouldConvertRailsRoutes4() {
    var pattern = RoutePatterns.railsRouteToRegex("/puppies/:id");
    var m = pattern.matcher("/puppies/42");
    Assertions.assertTrue(m.matches());
    Assertions.assertEquals("42", m.group("id"));
  }

  @Test
  void shouldConvertRailsRoutesGlob() {
    var pattern = RoutePatterns.railsRouteToRegex("/puppies/*world/");
    var m = pattern.matcher("/puppies/hello/world/");
    Assertions.assertTrue(m.matches());
  }
}
