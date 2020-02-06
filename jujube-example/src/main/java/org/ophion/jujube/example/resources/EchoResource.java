package org.ophion.jujube.example.resources;

import org.apache.hc.core5.http.Method;
import org.ophion.jujube.context.JujubeHttpContext;
import org.ophion.jujube.context.ParameterSource;
import org.ophion.jujube.http.HttpConstraints;
import org.ophion.jujube.response.ClientError;
import org.ophion.jujube.response.HttpResponseNotFound;
import org.ophion.jujube.response.JujubeHttpResponse;

public class EchoResource {

  public JujubeHttpResponse hello(JujubeHttpContext ctx) {
    if (Method.GET.isSame(ctx.getRequest().getMethod())) {
      return new JujubeHttpResponse("This resource requires a POST request and if you send us your name we'll greet you!");
    }

    var param = ctx.getParameter("name", ParameterSource.FORM)
      .orElseThrow(() -> new ClientError("visitor param is required"));

    return new JujubeHttpResponse("Well, hello there:" + param.asText());
  }

  public JujubeHttpResponse notFound(JujubeHttpContext ctx) {
    // constrains the allowed verbs to GET only:
    HttpConstraints.onlyAllowMethod(Method.GET, ctx);
    return new HttpResponseNotFound();
  }
}
