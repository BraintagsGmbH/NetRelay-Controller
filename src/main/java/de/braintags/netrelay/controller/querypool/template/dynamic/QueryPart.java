package de.braintags.netrelay.controller.querypool.template.dynamic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A part of a query. All parts together form the complete condition of this query. A part my be<br>
 * <ul>
 * <li>an 'and', requiring all conditions within to evaluate to 'true'</li>
 * <li>an 'or', requiring at least one condition within to evaluate to 'true'</li>
 * <li>a 'condition', representing a simple "field logic value" condition. The only one that does not contain further
 * query parts</li>
 * </ul>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */

public class QueryPart {
  private List<QueryPart> and;
  private List<QueryPart> or;
  private Condition       condition;

  /**
   * The constructor filling the fields of this part. Exactly one of the given parameters must not be null, the others
   * must be null.
   */
  @JsonCreator
  public QueryPart(@JsonProperty("and") List<QueryPart> and, @JsonProperty("or") List<QueryPart> or,
      @JsonProperty("condition") Condition condition) {
    assert (and != null ^ or != null ^ condition != null);
    this.and = and;
    this.or = or;
    this.condition = condition;
  }

  /**
   * @return if this query part is an "and" part
   */
  public boolean isAnd() {
    return and != null;
  }

  /**
   * @return if this query part is an "or" part
   */
  public boolean isOr() {
    return or != null;
  }

  /**
   * @return if this query part is a "condition" part
   */
  public boolean isCondition() {
    return condition != null;
  }

  /**
   * @return the query parts inside this "and" part
   */
  public List<QueryPart> getAnd() {
    assert isAnd();
    return and;
  }

  /**
   * @return the query parts inside this "or" part
   */
  public List<QueryPart> getOr() {
    assert isOr();
    return or;
  }

  /**
   * @return the condition
   */
  public Condition getCondition() {
    assert isCondition();
    return condition;
  }

}
