package org.ophion.jujube.middleware;

public class AccessLogMiddleware extends Middleware {
//  private LocalDateTime start;
//  private HttpRequest req;
//
//
//  @Override
//  public void onRequest(HttpRequest req) {
//    this.req = req;
//    this.start = LocalDateTime.now();
//  }
//
//  @Override
//  public void onResponse(HttpResponse resp) {
//    var line = String.format("%s %s %s (%s) → %s %s", start,
//      req.META.get(HttpRequest.Metadata.REMOTE_IP),
//      req.getMethod(),
//      req.getContentType().getMimeType(),
//      req.getPath(),
//      Durations.humanize(Duration.between(start, LocalDateTime.now())));
//
//    System.out.println(line);
//  }
}
