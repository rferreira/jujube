package org.ophion.jujube.internal;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.FileEntityProducer;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AbstractServerExchangeHandler;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static asset handler that feeds files in the main IO loop.
 * <p>
 * Todo:
 * Support byte ranges.
 * Support etags.
 * </p>
 */
public class JujubeAssetsServerExchangeHandler extends AbstractServerExchangeHandler<Message<HttpRequest, Void>> {
  private static final Logger LOG = Loggers.build();
  private static final Pattern SLASH_PATTERN = Pattern.compile("^/*(.*?)/*$");
  private final JujubeConfig config;
  private final String resourcePathPrefix;
  private final String indexFile;
  private final String externalBasePath;
  private final Path parentDir;

  public JujubeAssetsServerExchangeHandler(JujubeConfig config, String path, String resourcePathPrefix, String indexFile) {
    this.config = config;
    this.resourcePathPrefix = trimSlashes(resourcePathPrefix);
    this.indexFile = trimSlashes(indexFile);

    // sanitizing path:
    var splatIndex = path.indexOf("*");
    if (splatIndex > 0) {
      path = path.substring(0, splatIndex);
    }
    this.externalBasePath = trimSlashes(path);

    try {
      this.parentDir = Paths.get(getResource(this.resourcePathPrefix).toURI());
    } catch (URISyntaxException | NullPointerException e) {
      throw new IllegalStateException("Invalid resource path prefix " + resourcePathPrefix);
    }
  }

  private static String trimSlashes(String s) {
    final Matcher matcher = SLASH_PATTERN.matcher(s);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return s;
    }
  }

  @Override
  protected AsyncRequestConsumer<Message<HttpRequest, Void>> supplyConsumer(HttpRequest request, EntityDetails entityDetails, HttpContext context) {
    return new BasicRequestConsumer<>(new NoopEntityConsumer());
  }

  @Override
  protected void handle(Message<HttpRequest, Void> requestMessage, AsyncServerRequestHandler.ResponseTrigger responseTrigger, HttpContext context) throws IOException, HttpException {
    var request = requestMessage.getHead();

    try {
      var path = trimSlashes(request.getPath());

      // if not a GET or HEAD method pop:
      if (!(Method.GET.isSame(request.getMethod()) || Method.HEAD.isSame(request.getMethod()))) {
        responseTrigger.submitResponse(AsyncResponseBuilder.create(HttpStatus.SC_METHOD_NOT_ALLOWED).build(), context);
        return;
      }

      if (path != null && path.length() >= externalBasePath.length()) {
        var pathTryToLoad = resourcePathPrefix + "/" + trimSlashes(path.substring(externalBasePath.length()));
        var file = load(pathTryToLoad);

        if (file != null) {
          LOG.debug("file: {}", file);

          // ensuring that there're no shenanigans happening:
          if (!file.toPath().startsWith(parentDir)) {
            throw new IllegalStateException("A suspicious path request was blocked for path: " + pathTryToLoad);
          }

          // saving last modification stat:
          final var lastModified = Instant.ofEpochMilli(file.lastModified());

          // handle conditional requests:
          if (config.getStaticAssetsConfig().isIfModifiedSinceEnabled()) {
            // The If-Modified-Since request HTTP header makes the request conditional: the server will send back
            // the requested resource, with a 200 status, only if it has been last modified after the given date
            var ifModifiedHeader = request.getFirstHeader(HttpHeaders.IF_MODIFIED_SINCE);
            if (ifModifiedHeader != null) {
              var targetInstant = LocalDateTime.parse(ifModifiedHeader.getValue(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC);
              if (!lastModified.isAfter(targetInstant)) {
                LOG.debug("content with modification date {} is not after last modified header {}", lastModified, targetInstant);
                responseTrigger.submitResponse(AsyncResponseBuilder.create(HttpStatus.SC_NOT_MODIFIED).build(), context);
                return;
              }
            }

            // The If-Unmodified-Since request HTTP header makes the request conditional: the server will send back the
            // requested resource, or accept it in the case of a POST or another non-safe method, only if it has not
            // been last modified after the given date. If the resource has been modified after the given date,
            // the response will be a 412 (Precondition Failed) error.
            var ifUnmodifiedHeader = request.getFirstHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
            if (ifUnmodifiedHeader != null) {
              var targetInstant = LocalDateTime.parse(ifUnmodifiedHeader.getValue(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC);
              if (lastModified.isAfter(targetInstant)) {
                LOG.debug("content with modification date {} is not before last unmodified header {}", lastModified, targetInstant);
                responseTrigger.submitResponse(AsyncResponseBuilder.create(HttpStatus.SC_PRECONDITION_FAILED).build(), context);
                return;
              }
            }
          }

          // now we have a file, so lets' lookup the mimeType:
          var extension = getFileExtension(file);

          var contentType = ContentType.APPLICATION_OCTET_STREAM;

          if (config.getStaticAssetsConfig().getExtensionToMimeMapping().containsKey(extension)) {
            contentType = ContentType.parseLenient(config.getStaticAssetsConfig().getExtensionToMimeMapping().get(extension));
            if (contentType.toString().startsWith("text/")) {
              contentType = contentType.withCharset(config.getDefaultCharset());
            }

            LOG.debug("mapping extension: {} to content type: {}", extension, contentType);
          }

          var responseBuilder = AsyncResponseBuilder.create(HttpStatus.SC_OK);

          // entity:
          if (Method.GET.isSame(request.getMethod())) {
            responseBuilder.setEntity(new FileEntityProducer(file, contentType));
          }

          // last modified header:
          if (config.getStaticAssetsConfig().isLastModifiedEnabled()) {
            responseBuilder.addHeader(HttpHeaders.LAST_MODIFIED,
              DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC))
            );
          }

          responseTrigger.submitResponse(responseBuilder.build(), context);
        }
      }
    } catch (URISyntaxException | RuntimeException e) {
      e.printStackTrace();
    }
    responseTrigger.submitResponse(AsyncResponseBuilder.create(HttpStatus.SC_NOT_FOUND)
        .build(),
      context);
  }

  /**
   * Returns a {@code URL} pointing to {@code resourceName} if the resource is found using the
   * {@linkplain Thread#getContextClassLoader() context class loader}. In simple environments, the
   * context class loader will find resources from the class path. In environments where different
   * threads can have different class loaders, for example app servers, the context class loader
   * will typically have been set to an appropriate loader for the current thread.
   *
   * <p>In the unusual case where the context class loader is null, the class loader that loaded
   * this class ({@code JujubeAssetsServerExchangeHandler}) will be used instead.
   *
   * @return null when resource is not found.
   */
  private URL getResource(String resourceName) {
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    final ClassLoader loader = contextClassLoader == null ? JujubeAssetsServerExchangeHandler.class.getClassLoader() : contextClassLoader;
    return loader.getResource(resourceName);
  }

  private File load(String path) throws URISyntaxException {
    LOG.debug("trying to load:{}", path);
    var url = getResource(path);
    if (url == null) {
      return null;
    }
    var fd = new File(url.toURI());
    if (fd.isDirectory()) {
      if (indexFile != null) {
        LOG.debug("path is a directory, looking up index file {}", indexFile);
        fd = new File(getResource(path + "/" + indexFile).toURI());
      } else {
        fd = null;
      }
    }
    return fd;
  }

  private String getFileExtension(File file) {
    String name = file.getName();
    int lastIndexOf = name.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return ""; // empty extension
    }
    return name.substring(lastIndexOf + 1);
  }
}
