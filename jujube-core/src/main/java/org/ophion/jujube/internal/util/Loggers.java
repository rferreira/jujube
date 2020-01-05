package org.ophion.jujube.internal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loggers {
  private static final Logger LOG = LoggerFactory.getLogger(Loggers.class);

  public static Logger build() {
    StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    Class<?> cls = walker.getCallerClass();
    LOG.trace("building LOGGER for {}", cls.getName());
    return LoggerFactory.getLogger(cls.getName());
  }
}
