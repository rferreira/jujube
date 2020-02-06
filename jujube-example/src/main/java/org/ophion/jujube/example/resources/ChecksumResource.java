package org.ophion.jujube.example.resources;

import org.apache.commons.codec.binary.Hex;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.ophion.jujube.context.FileParameter;
import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.context.ParameterSource;
import org.ophion.jujube.http.HttpConstraints;
import org.ophion.jujube.http.HttpResponses;
import org.ophion.jujube.response.ClientError;
import org.ophion.jujube.response.JujubeHttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Sample resource that calculates the checksum of a file.
 */
public class ChecksumResource {
  public JujubeHttpResponse post(JujubeHttpContext ctx) throws NoSuchAlgorithmException, IOException {
    HttpConstraints.onlyAllowMediaType(ContentType.MULTIPART_FORM_DATA, ctx);
    HttpConstraints.onlyAllowMethod(Method.POST, ctx);

    var file = (FileParameter) ctx.getParameter("file", ParameterSource.FORM)
      .orElseThrow(() -> new ClientError("Oops, you must supply a file argument"));

    var hash = ctx.getParameter("file", ParameterSource.FORM);

    var digest = MessageDigest.getInstance("sha256");

    if (hash.isPresent()) {
      digest = MessageDigest.getInstance(hash.get().asText());
    }

    try (var ins = new BufferedInputStream(Files.newInputStream(file.asPath()))) {
      while (ins.available() > 0) {
        digest.update((byte) ins.read());
      }
    }

    var checksum = Hex.encodeHexString(digest.digest());
    return HttpResponses.ok(String.format("checksum: %s", checksum));
  }
}
