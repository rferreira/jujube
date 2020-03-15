package org.ophion.jujube.example.resources;

import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.http.HttpConstraints;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.request.ParameterSource;
import org.ophion.jujube.response.ClientError;
import org.ophion.jujube.response.HttpResponses;
import org.ophion.jujube.response.JujubeResponse;

public class EchoResource {

  public JujubeResponse hello(JujubeRequest req, HttpContext ctx) {
    if (Method.GET.isSame(req.getMethod())) {
      return new JujubeResponse("This resource requires a POST request and if you send us your name we'll greet you!");
    }

    var param = req.getParameter("name", ParameterSource.FORM)
      .orElseThrow(() -> new ClientError("name param is required"));

    return new JujubeResponse(String.format("Well, hello there: %s!\n", param.asText()));
  }

  public JujubeResponse notFound(JujubeRequest req, HttpContext ctx) {
    // constrains the allowed verbs to GET only:
    HttpConstraints.onlyAllowMethod(Method.GET, req);

    if (req.getMethod().equals("GET")) {
      return HttpResponses.noContent();
    }

    return HttpResponses.notFound();
  }
}
