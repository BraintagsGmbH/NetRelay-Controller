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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.braintags.netrelay.controller.querypool.exceptions.InvalidSyntaxException;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.dataaccess.query.QueryOperator;
import de.braintags.vertx.jomnigate.dataaccess.query.impl.QueryOr;

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
@JsonInclude(value = Include.NON_NULL)
public class QueryPart {
  private List<QueryPart> and;
  private List<QueryPart> or;
  private Condition condition;

  /**
   * The constructor filling the fields of this part. Exactly one of the given parameters must not be null, the others
   * must be null.
   *
   * @throws InvalidSyntaxException
   */
  @JsonCreator
  public QueryPart(@JsonProperty("and") List<QueryPart> and, @JsonProperty("or") List<QueryPart> or,
      @JsonProperty("condition") Condition condition) throws InvalidSyntaxException {
    if (and != null ^ or != null ^ condition != null ^ (and != null && or != null && condition != null)) {
      this.and = and;
      this.or = or;
      this.condition = condition;
    } else {
      throw new InvalidSyntaxException(
          "A query part must consist of exactly one 'and', 'or', or 'condition': " + and + " " + or + " " + condition);
    }
  }

  /**
   * Recursively convert this query part to an {@link ISearchCondition}
   *
   * @throws InvalidSyntaxException
   *           if the part is of an unknown type
   */
  public ISearchCondition toSearchCondition() throws InvalidSyntaxException {
    if (isAnd()) {
      List<QueryPart> queryParts = getAnd();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = queryParts.get(i).toSearchCondition();
        searchConditions[i] = subQueryPart;
      }
      return ISearchCondition.and(searchConditions);
    } else if (isOr()) {
      List<QueryPart> queryParts = getOr();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = queryParts.get(i).toSearchCondition();
        searchConditions[i] = subQueryPart;
      }
      return new QueryOr(searchConditions);
    } else if (isCondition()) {
      Condition condition = getCondition();
      String field = condition.getField();
      QueryOperator operator = condition.getLogic();
      Object value = condition.getValue();
      // XXX a new IIndexedField is created here, better would be to check if there is an IIndexedField and fail if not
      return ISearchCondition.condition(field, operator, value);
    } else {
      throw new InvalidSyntaxException("Query part is neither 'and' nor 'or' nor 'condition'");
    }
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
