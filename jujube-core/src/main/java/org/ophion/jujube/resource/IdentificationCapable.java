package org.ophion.jujube.resource;

public interface IdentificationCapable<T> {
  String getId();

  T getItem();
}
