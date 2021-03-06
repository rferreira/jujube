package org.ophion.jujube.example.resources;

import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.http.HttpConstraints;
import org.ophion.jujube.request.FileParameter;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.response.ClientError;
import org.ophion.jujube.response.HttpResponses;
import org.ophion.jujube.response.JujubeResponse;
import org.ophion.jujube.util.DataSize;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sample resource that calculates the checksum of a file.
 * <p>
 * $ curl -F "file=@/Users/rafael/Downloads/output.pdf" -vk https://localhost:8080/checksum/
 */
public class ChecksumResource {
  public JujubeResponse post(JujubeRequest req, HttpContext ctx) throws NoSuchAlgorithmException, IOException {
    HttpConstraints.onlyAllowMediaType(ContentType.MULTIPART_FORM_DATA, req);

    if (Method.GET.isSame(req.getMethod())) {
      var availableHashes = Arrays.stream(Security.getProviders())
        .flatMap(provider -> provider.getServices().stream())
        .filter(s -> MessageDigest.class.getSimpleName().equals(s.getType()))
        .map(Provider.Service::getAlgorithm)
        .collect(Collectors.joining(","));

      return HttpResponses.ok(JSON.std.asString(Map.of(
        "error", "Checksum calculating resource, please send us a file, and an optional hash to use",
        "available algorithms", availableHashes,
        "available arguments", List.of("file - required", "hash - optional"),
        "required encoding", ContentType.MULTIPART_FORM_DATA.getMimeType())
        ), ContentType.APPLICATION_JSON
      );
    }

    var file = (FileParameter) req.getParameter("file", ParameterSource.FORM)
      .orElseThrow(() -> new ClientError("Oops, you must supply a file argument"));

    var hash = req.getParameter("hash", ParameterSource.FORM);
    var digest = MessageDigest.getInstance("SHA-256");

    try {
      if (hash.isPresent()) {
        digest = MessageDigest.getInstance(hash.get().asText());
      }

    } catch (NoSuchAlgorithmException ex) {
      return HttpResponses.badRequest(String.format("error: %s \n", ex.getMessage()));
    }
    DigestUtils.digest(digest, file.asPath().toFile());

    var checksum = Hex.encodeHexString(digest.digest());
    return new JujubeResponse(200, JSON.std.asString(Map.of(
      "algorithm", digest.getAlgorithm(),
      "checksum:", checksum,
      "size", DataSize.bytes(Files.size(file.asPath())).toString()
    )), ContentType.APPLICATION_JSON);


  }
}
