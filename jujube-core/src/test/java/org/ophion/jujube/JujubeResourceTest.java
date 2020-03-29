package org.ophion.jujube;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ophion.jujube.http.ContentTypes;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.resource.IdentificationCapable;
import org.ophion.jujube.resource.JujubeResource;
import org.ophion.jujube.resource.PaginationCapable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class JujubeResourceTest extends IntegrationTest {
  public static final String BASE_PATH = "/v1.0/puppies";
  private static final Logger LOG = Loggers.build();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldRouteActions() throws IOException {
    config.route(new PuppyResource());
    server.start();

    // creating some resources:
    var req = new HttpPost(endpoint.resolve(BASE_PATH));
    req.setEntity(MultipartEntityBuilder.create()
      .addTextBody("name", "theodore")
      .addTextBody("breed", "st bernard")
      .build()
    );

    AtomicReference<String> location = new AtomicReference<>();
    var contents = client.execute(req, response -> {
      // expected 201 with location header:
      Assertions.assertEquals(HttpStatus.SC_CREATED, response.getCode());
      Assertions.assertNotNull(response.getHeader(HttpHeaders.LOCATION));
      location.set(response.getHeader(HttpHeaders.LOCATION).getValue());
      return EntityUtils.toString(response.getEntity());
    });

    var resource = objectMapper.readValue(contents, Puppy.class);
    Assertions.assertEquals(resource.getName(), "theodore");
    Assertions.assertEquals(resource.getBreed(), "st bernard");

    // retrieving resource:
    contents = client.execute(new HttpGet(location.get()), response -> {
      // expected 200 with JSON content
      Assertions.assertEquals(HttpStatus.SC_OK, response.getCode());
      Assertions.assertEquals(ContentType.APPLICATION_JSON.withCharset(config.getDefaultCharset()).toString(), response.getEntity().getContentType());
      return EntityUtils.toString(response.getEntity());
    });
    Assertions.assertEquals(objectMapper.writeValueAsString(resource), contents.trim());
  }

  // our model:
  static class Puppy {
    private int id;
    private String name;
    private String breed;

    public Puppy() {
    }

    Puppy(int id, String name, String breed) {
      this.id = id;
      this.name = name;
      this.breed = breed;
    }

    public String getName() {
      return name;
    }

    public String getBreed() {
      return breed;
    }

    public int getId() {
      return id;
    }
  }

  static class PuppyResource extends JujubeResource<Puppy> {
    private List<Puppy> puppyList;
    private AtomicInteger counter = new AtomicInteger();

    protected PuppyResource() {
      super(BASE_PATH, ContentTypes.ANY_TYPE, ContentType.APPLICATION_JSON);
      puppyList = new ArrayList<>();
    }

    @Override
    public IdentificationCapable<Puppy> create(JujubeRequest req, HttpContext ctx) {
      var entity = req.getHttpEntity().orElseThrow();
      var item = new Puppy(counter.incrementAndGet(),
        req.getParameter("name", ParameterSource.FORM).orElseThrow().asText(),
        req.getParameter("breed", ParameterSource.FORM).orElseThrow().asText());
      puppyList.add(item);

      return new IdentificationCapable<>() {
        @Override
        public String getId() {
          return String.valueOf(item.getId());
        }

        @Override
        public Puppy getItem() {
          return item;
        }
      };
    }

    @Override
    public PaginationCapable<Puppy> index(String continuationToken, Integer maxPageSize, JujubeRequest req, HttpContext context) {
      return new PaginationCapable<>() {
        @Override
        public String getContinuationToken() {
          return Base64.getEncoder().encodeToString("42".getBytes());
        }

        @Override
        public List<Puppy> getValues() {
          return puppyList;
        }
      };
    }

    @Override
    public IdentificationCapable<Puppy> retrieve(JujubeRequest req, HttpContext ctx) {
      var id = req.getParameter("id", ParameterSource.PATH).orElseThrow();
      var item = puppyList.stream()
        .filter(i -> i.getId() == id.asInteger())
        .findFirst()
        .orElse(null);

      return new IdentificationCapable<>() {
        @Override
        public String getId() {
          return id.asText();
        }

        @Override
        public Puppy getItem() {
          return item;
        }
      };
    }

    @Override
    public Puppy decode(String source) {
      try {
        return objectMapper.readValue(source, Puppy.class);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String encode(Object source) {
      try {
        return objectMapper.writeValueAsString(source);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
