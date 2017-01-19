package de.braintags.netrelay.controller.querypool.template.dynamic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple query condition, in the form of <code>"Field" "Logic" "Value"</code>.<br>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */

public class Condition {
  private String field;
  private String logic;
  private Object value;

  /**
   * The constructor filling all fields. Field and logic are required, value is optional.
   *
   * @param field
   *          the field name of this condition
   * @param logic
   *          the logic of this condition
   * @param value
   *          the value of this condition, can be null
   */
  @JsonCreator
  public Condition(@JsonProperty(value = "field", required = true) String field,
      @JsonProperty(value = "logic", required = true) String logic, @JsonProperty("value") Object value) {
    this.field = field;
    this.logic = logic;
    this.value = value;
  }

  /**
   * @return the field name of this condition
   */
  public String getField() {
    return field;
  }

  /**
   * @return the logic of this condition
   */
  public String getLogic() {
    return logic;
  }

  /**
   * @return the value of this condition, can be null
   */
  public Object getValue() {
    return value;
  }
}
