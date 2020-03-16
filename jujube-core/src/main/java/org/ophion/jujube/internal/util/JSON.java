package org.ophion.jujube.internal.util;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Crude, yet self-sufficient, JSON encoding.
 */
public final class JSON {
  // we ignore all base object properties:
  // TODO: convert this into an array
  private static List<String> PROPERTIES_TO_IGNORE;

  static {
    try {
      var info = Introspector.getBeanInfo(Object.class);
      var pds = info.getPropertyDescriptors();
      PROPERTIES_TO_IGNORE = Stream.of(pds)
        .map(FeatureDescriptor::getName)
        .collect(Collectors.toUnmodifiableList());

    } catch (IntrospectionException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String stringify(Map<Object, Object> contents) {
    // order items:
    var orderedView = new TreeMap<>(contents);

    StringBuilder buffer = new StringBuilder();
    buffer.append("{");
    Iterator<Map.Entry<Object, Object>> it = orderedView.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Object, Object> pair = it.next();
      buffer.append("\"");
      buffer.append(pair.getKey().toString());
      buffer.append("\"");
      buffer.append(":");
      buffer.append("\"");
      buffer.append(pair.getValue().toString());
      buffer.append("\"");

      if (it.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append("}");

    return buffer.toString();
  }

  public static String stringify(Object contents) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("{");

    try {
      BeanInfo info = Introspector.getBeanInfo(contents.getClass());
      var pds = info.getPropertyDescriptors();

      Iterator<PropertyDescriptor> it = Arrays.asList(pds).iterator();
      while (it.hasNext()) {
        var pd = it.next();

        if (PROPERTIES_TO_IGNORE.contains(pd.getName())) {
          continue;
        }

        buffer.append("\"");
        buffer.append(pd.getDisplayName());
        buffer.append("\"");
        buffer.append(":");
        buffer.append("\"");
        buffer.append(pd.getReadMethod().invoke(contents).toString());
        buffer.append("\"");

        if (it.hasNext()) {
          buffer.append(", ");
        }
      }


    } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }

    buffer.append("}");
    return buffer.toString();
  }
}
