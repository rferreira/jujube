package org.ophion.jujube.resource;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.ophion.jujube.internal.util.Loggers;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.response.HttpResponses;
import org.ophion.jujube.response.JujubeHttpException;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.routing.Route;
import org.ophion.jujube.routing.RouteGroup;
import org.ophion.jujube.routing.RoutePatterns;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.hc.core5.http.Method.valueOf;

/**
 * This is a boilerplate resource aimed at speeding up the development of an API controller. Its goal is to follow,
 * as close as reasonably possible, Microsoft's REST guidelines.
 * <p>
 * {@see https://github.com/microsoft/api-guidelines/}
 * {@see https://docs.microsoft.com/en-us/azure/architecture/best-practices/api-design}
 *
 * @param <T> the model this resource produces/consumes.
 */
public abstract class JujubeResource<T> implements RouteGroup {
  private static final Logger LOG = Loggers.build();
  private static DateTimeFormatter RFC_5322 = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
  private final ContentType contentTypeProduced;
  private final ContentType contentTypeConsumed;
  private final URI basePath;

  protected JujubeResource(String basePath, ContentType consumes, ContentType produces) {
    this.basePath = URI.create(basePath);
    this.contentTypeConsumed = consumes;
    this.contentTypeProduced = produces;
  }

  @Override
  public List<Route> getRouteGroup() {
    return List.of(
      new Route(RoutePatterns.railsRouteToRegex(basePath.toString()), this::dispatch),
      new Route(RoutePatterns.railsRouteToRegex(basePath.resolve(basePath.getPath() + "/:id").toString()), this::dispatchResource)
    );
  }

  public PaginationCapable<T> index(String continuationToken, Integer maxPageSize, JujubeRequest req, HttpContext context) {
    throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  public IdentificationCapable<T> retrieve(JujubeRequest req, HttpContext ctx) {
    throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  public IdentificationCapable<T> create(JujubeRequest req, HttpContext ctx) {
    throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  public IdentificationCapable<T> update(JujubeRequest req, HttpContext ctx) {
    throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  public boolean destroy(JujubeRequest req, HttpContext ctx) {
    throw new JujubeHttpException(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * Routes the request inside a resource.
   * <p>
   * {@see https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#74-supported-methods}
   *
   * @param req the HTTP request.
   * @param ctx the HTTP context.
   * @return a Jujube response.
   */
  public JujubeResponse dispatch(JujubeRequest req, HttpContext ctx) throws URISyntaxException {
    LOG.debug("dispatching multi resource for {}", req.getPath());

    final var response = HttpResponses.notFound();
    populateStandardResponseHeaders(response);

    switch (valueOf(req.getMethod())) {
      case GET:
        // GET should always return a 200:
        LOG.debug("index request");
        AtomicReference<String> continuationToken = new AtomicReference<>();
        AtomicReference<Integer> maxPageSize = new AtomicReference<>();
        req.getParameter("$token", ParameterSource.QUERY).ifPresent(t -> continuationToken.set(t.asText()));
        req.getParameter("$maxpagesize", ParameterSource.QUERY).ifPresent(p -> maxPageSize.set(p.asInteger()));
        var pc = index(continuationToken.get(), maxPageSize.get(), req, ctx);

        var content = new HashMap<String, Object>();
        content.put("value", pc.getValues());
        if (pc.getContinuationToken() != null) {
          content.put("@nextLink", new URIBuilder(basePath)
            .setParameter("token", pc.getContinuationToken()).build().toString());
        }
        response.setContent(encode(content));
        break;
      case POST:
      case PUT:
        // https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#741-post
        // PUT or POST that create a new resource return a 201 while an update returns a 200
        IdentificationCapable<T> item = create(req, ctx);
        var location = new URIBuilder(req.getUri())
          .setPath(basePath + "/" + item.getId())
          .build().toString();
        response.setHeader(HttpHeaders.LOCATION, location);
        response.setCode(HttpStatus.SC_CREATED);
        response.setContent(encode(item.getItem()));
        break;
    }

    return response;
  }

  private JujubeResponse dispatchResource(JujubeRequest req, HttpContext ctx) {
    LOG.debug("dispatching individual resource for {}", req.getPath());

    final var response = HttpResponses.notFound();
    populateStandardResponseHeaders(response);

    IdentificationCapable<T> identificationCapable = null;

    switch (valueOf(req.getMethod())) {
      case GET:
        // GET should always return a 200:
        LOG.debug("retrieve request");
        identificationCapable = retrieve(req, ctx);

        if (identificationCapable.getItem() != null) {
          response.setContent(encode(identificationCapable.getItem()));
          response.setCode(HttpStatus.SC_OK);
        } // else it will return a not found
        break;
      case POST:
      case PUT:
      case PATCH:
        // PATH that updates an existing resource returns a 200
        identificationCapable = update(req, ctx);
        response.setHeader(HttpHeaders.LOCATION, basePath.resolve("/" + identificationCapable.getId()).toString());
        response.setCode(HttpStatus.SC_OK);
        response.setContent(encode(identificationCapable.getItem()));
        break;
      case DELETE:
        // If the delete operation is successful, the web server should respond with HTTP status code 204,
        // indicating that the process has been successfully handled, but that the response body contains no further
        // information. If the resource doesn't exist, the web server can return HTTP 404 (Not Found).
        if (destroy(req, ctx)) {
          response.setCode(HttpStatus.SC_NO_CONTENT); // 204
        } else {
          response.setCode(HttpStatus.SC_NOT_FOUND); //
        }
      case HEAD:
        LOG.debug("Handle head!");
        break;
    }

    return response;
  }

  /**
   * Populate standard response headers as specified in
   * https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#76-standard-response-headers
   *
   * @param response the response to hydrate
   */
  private void populateStandardResponseHeaders(JujubeResponse response) {
    response.addHeader(new BasicHeader(HttpHeaders.DATE, RFC_5322.format(Instant.now())));
    response.setContentType(contentTypeProduced);
    //TODO: add content encoding, preference applied and etag
  }

  public abstract T decode(String source);

  public abstract String encode(Object source);
}
