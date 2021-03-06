/*-
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.querypool.template.dynamic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.braintags.vertx.jomnigate.dataaccess.query.QueryOperator;

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
  private QueryOperator logic;
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
      @JsonProperty(value = "logic", required = true) QueryOperator logic, @JsonProperty("value") Object value) {
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
  public QueryOperator getLogic() {
    return logic;
  }

  /**
   * @return the value of this condition, can be null
   */
  public Object getValue() {
    return value;
  }
}
