package org.ophion.jujube.internal.multipart;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeaderValueParser;
import org.apache.hc.core5.http.message.ParserCursor;
import org.ophion.jujube.internal.util.Loggers;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PartMetadata {
  public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
  private static final Pattern SPLIT_HEADER = Pattern.compile("^(.*): (.*)$");
  private static final Logger LOG = Loggers.build();
  private final String name;
  private final String filename;
  private final ContentType contentType;
  private final Map<String, String> headers;


  public PartMetadata(String metadata) {
    AtomicReference<String> name = new AtomicReference<>();
    AtomicReference<String> filename = new AtomicReference<>();
    // RFC1867 - defaults to text/plain:
    ContentType contentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType());
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


    var lines = metadata.trim().split("\n");
    for (String line : lines) {
      LOG.debug("parsing header line: {}", line);
      var matcher = SPLIT_HEADER.matcher(line.trim());
      if (matcher.matches()) {
        headers.put(matcher.group(1), matcher.group(2));
      } else {
        LOG.warn("This malformed header, will be ignored: {}", line);
      }
    }

    if (headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
      contentType = ContentType.parse(headers.get(HttpHeaders.CONTENT_TYPE));
    }
    if (headers.containsKey(HEADER_CONTENT_DISPOSITION)) {
      var contents = headers.get(HEADER_CONTENT_DISPOSITION);
      var pairs = BasicHeaderValueParser.INSTANCE.parseParameters(contents, new ParserCursor(0, contents.length()));
      Stream.of(pairs).forEach(pair -> {
        if (pair.getName().equalsIgnoreCase("name")) {
          name.set(pair.getValue());
        }

        if (pair.getName().equalsIgnoreCase("filename")) {
          filename.set(pair.getValue());
        }
      });
    }

    this.name = name.get();
    this.filename = filename.get();
    this.contentType = contentType;
    this.headers = headers;
  }

  public String getName() {
    return name;
  }

  public String getFilename() {
    return filename;
  }

  public ContentType getContentType() {
    return contentType;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public boolean isText() {
    return contentType.toString().toLowerCase().startsWith("text/");
  }
}
