package com.datastax.driver.core;

import org.assertj.core.api.Condition;

class ContainsAnyStringCondition extends Condition<String> {

  private final String[] stringsToMatch;

  ContainsAnyStringCondition(String... stringsToMatch) {
    this.stringsToMatch = stringsToMatch;
  }

  @Override
  public boolean matches(String value) {
    for (String toMatch : stringsToMatch) {
      if (value.contains(toMatch)) {
        return true;
      }
    }
    return false;
  }
}
