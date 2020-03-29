package org.ophion.jujube.resource;

import java.util.List;

/**
 * {@see https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#981-server-driven-paging}
 *
 * @param <T>
 */
public interface PaginationCapable<T> {
  String getContinuationToken();

  List<T> getValues();
}
