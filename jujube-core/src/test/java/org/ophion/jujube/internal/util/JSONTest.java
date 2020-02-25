package org.ophion.jujube.internal.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class JSONTest {
  @Test
  void shouldConvertToJson() {
    var json = JSON.stringify(Map.of("name", "Bob", "color", "blue"));
    Assertions.assertEquals("abc", json);
  }

  @Test
  void shouldConvertBeansToJson() {
    var json = JSON.stringify(new Human("Bob", 42, "secret"));
    Assertions.assertEquals("{\"age\":\"42\", \"name\":\"Bob\"}", json);

  }

  private static class Human {
    private String name;
    private int age;
    private String internal;

    public Human(String name, int age, String internal) {
      this.name = name;
      this.age = age;
      this.internal = internal;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }
}
