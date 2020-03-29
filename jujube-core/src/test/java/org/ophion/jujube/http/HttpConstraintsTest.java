package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeHttpException;

import java.util.Optional;

class HttpConstraintsTest {

  @Test
  void shouldNotBlockIfTypeMatches() {
    var req = Mockito.mock(JujubeRequest.class);
    var entity = HttpEntities.create("hello", ContentType.TEXT_PLAIN);

    Mockito.when(req.getHttpEntity()).thenReturn(Optional.of(entity));
    HttpConstraints.onlyAllowMediaType(ContentTypes.ANY_TYPE, req);
  }

  @Test
  void shouldNotBlockIfSubTypeMatches() {
    var req = Mockito.mock(JujubeRequest.class);
    var entity = HttpEntities.create("hello", ContentType.TEXT_PLAIN);

    Mockito.when(req.getHttpEntity()).thenReturn(Optional.of(entity));
    HttpConstraints.onlyAllowMediaType(ContentTypes.ANY_TEXT_TYPE, req);
  }

  @Test
  void shouldBlockIfTypeDoesNotMatch() {
    var req = Mockito.mock(JujubeRequest.class);
    var entity = HttpEntities.create("hello", ContentType.TEXT_PLAIN);

    Mockito.when(req.getHttpEntity()).thenReturn(Optional.of(entity));
    Assertions.assertThrows(JujubeHttpException.class, () -> {
      HttpConstraints.onlyAllowMediaType(ContentType.APPLICATION_JSON, req);
    });
  }

  @Test
  void shouldBlockIfSubTypeDoesNotMatch() {
    var req = Mockito.mock(JujubeRequest.class);
    var entity = HttpEntities.create("hello", ContentType.APPLICATION_XML);

    Mockito.when(req.getHttpEntity()).thenReturn(Optional.of(entity));
    Assertions.assertThrows(JujubeHttpException.class, () -> {
      HttpConstraints.onlyAllowMediaType(ContentType.APPLICATION_JSON, req);
    });
  }
}
