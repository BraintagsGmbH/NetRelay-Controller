package de.braintags.netrelay.controller.querypool.template;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart;

/**
 * The dynamic query part of the {@link QueryTemplate}. The dynamic query will be translated to a {@link IQuery}, which
 * should always work no matter the current datastore.<br>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */
public class DynamicQuery {

  @JsonProperty
  private String    orderBy;
  @JsonProperty(value = "query")
  private QueryPart rootQueryPart;

  /**
   * @return the order fields of the query. Should be in the format<br>
   *         <code>Fieldname [ASC|DESC], Fieldname [ASC|DESC],...</code>
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * @return the first {@link QueryPart} of the query, which might either contain a condition or more query parts
   */
  public QueryPart getRootQueryPart() {
    return rootQueryPart;
  }
}