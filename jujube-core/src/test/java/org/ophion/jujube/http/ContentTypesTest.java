package org.ophion.jujube.http;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ContentTypesTest {

    @Test
    void shouldProperlyCalculateRange() {
      Assertions.assertTrue(ContentTypes.isInRange(ContentType.APPLICATION_ATOM_XML, ContentTypes.parse("application/*")));
      Assertions.assertFalse(ContentTypes.isInRange(ContentType.APPLICATION_ATOM_XML, ContentTypes.parse("application/javascript")));
      Assertions.assertTrue(ContentTypes.isInRange(ContentTypes.ANY_TYPE, ContentTypes.ANY_TYPE));
      Assertions.assertTrue(ContentTypes.isInRange(ContentTypes.ANY_AUDIO_TYPE, ContentTypes.ANY_TYPE));
      Assertions.assertTrue(ContentTypes.isInRange(ContentTypes.parse("hello"), ContentTypes.ANY_TYPE));

      Assertions.assertFalse(ContentTypes.isInRange(ContentTypes.TEXT_HML, ContentTypes.APPLICATION_XML));
      Assertions.assertFalse(ContentTypes.isInRange(ContentTypes.TEXT_HML, ContentTypes.ANY_IMAGE_TYPE));

      // we might need to double check this one:
      Assertions.assertFalse(ContentTypes.isInRange(ContentType.parse("foo/bar"), ContentType.parse("foo")));

    }
}
