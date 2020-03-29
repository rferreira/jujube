package org.ophion.jujube.middleware;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.ophion.jujube.request.JujubeRequest;
import org.ophion.jujube.response.JujubeResponse;


public abstract class Middleware {
  public void onBeforeContentConsumption(HttpRequest request, HttpContext context) {

  }

  public void onAfterHandler(JujubeRequest request, JujubeResponse response, HttpContext context) {

  }

  public void onException() {

  }
}
