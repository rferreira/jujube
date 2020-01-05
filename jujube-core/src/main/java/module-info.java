module org.ophion.jujube {
  requires httpcore5;
  requires httpcore5.h2;
  requires org.slf4j;
  requires org.conscrypt;
  requires org.bouncycastle.provider;
  requires org.bouncycastle.pkix;

  // exports:
  exports org.ophion.jujube;
  exports org.ophion.jujube.response;
  exports org.ophion.jujube.middleware;
  exports org.ophion.jujube.config;
  exports org.ophion.jujube.http;
}
