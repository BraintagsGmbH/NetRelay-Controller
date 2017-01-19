package de.braintags.netrelay.controller.querypool.template;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.netrelay.controller.querypool.QueryPoolController.Operation;
import de.braintags.netrelay.controller.querypool.exceptions.InvalidSyntaxException;
import io.vertx.core.json.JsonObject;

/**
 * Java representation of the JSON that builds the query. Configured to be deserialized with Jackson JSON Parser<br>
 * <br>
 * Copyright: Copyright (c) 15.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */

public class QueryTemplate {

  private String mapper;
  private String description;
  private Operation operation;
  private List<NativeQuery> nativeQueries;
  private DynamicQuery dynamicQuery;

  private String orderBy;
  private Integer offset;
  private Integer limit;

  @JsonIgnore
  private JsonObject source;

  /**
   * Default constructor that fills all required fields. Each value is required to be not null, except native and
   * dynamic queries. Exactly one of native or dynamic query must not be null.
   *
   * @param mapper
   *          the name of the {@link IMapper}
   * @param description
   *          the description of the general use of this query
   * @param operation
   *          one of {@link Operation} that should be executed with this query
   * @param nativeQueries
   *          an optional list of {@link NativeQuery}s, one for each datastore that should be able to execute it
   * @param dynamicQuery
   *          a {@link DynamicQuery} to be used independently from the currently configured datastore
   * @throws InvalidSyntaxException
   *           if there is a syntax error in the template
   */
  @JsonCreator
  public QueryTemplate(@JsonProperty(value = "mapper", required = true) String mapper,
      @JsonProperty(value = "description", required = true) String description,
      @JsonProperty(value = "operation", required = true) Operation operation,
      @JsonProperty(value = "native") List<NativeQuery> nativeQueries,
      @JsonProperty(value = "dynamic") DynamicQuery dynamicQuery, @JsonProperty(value = "orderBy") String orderBy,
      @JsonProperty(value = "offset") Integer offset, @JsonProperty(value = "limit") Integer limit)
      throws InvalidSyntaxException {
    if (dynamicQuery != null && nativeQueries != null) {
      throw new InvalidSyntaxException("A query template can not define a dynamic and native query at the same time");
    }

    this.mapper = mapper;
    this.description = description;
    this.operation = operation;
    this.nativeQueries = nativeQueries;
    this.dynamicQuery = dynamicQuery;
    this.orderBy = orderBy;
    this.offset = offset;
    this.limit = limit;
  }

  /**
   * @return the name of the {@link IMapper}
   */
  public String getMapper() {
    return mapper;
  }

  /**
   * @return the description of the general use of this query
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return a list of {@link NativeQuery}s, one for each datastore that should be able to execute it. Either this or
   *         the {@link #getDynamicQuery()} must exist
   */
  public List<NativeQuery> getNativeQueries() {
    return nativeQueries;
  }

  /**
   * @return the {@link Operation} that should be executed for this query
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * @return the {@link DynamicQuery} to be used independently from the currently configured datastore. Either this or
   *         the {@link #getNativeQueries()} must exist
   */
  public DynamicQuery getDynamicQuery() {
    return dynamicQuery;
  }

  /**
   * @return the source JSON that was used to create this object, saved for reference purpose only
   */
  public JsonObject getSource() {
    return source;
  }

  /**
   * @param source
   *          the source JSON that was used to create this object, saved for reference purpose only
   */
  public void setSource(JsonObject source) {
    this.source = source;
  }

  /**
   * @return the order fields of the query. Should be in the format<br>
   *         <code>Fieldname [ASC|DESC], Fieldname [ASC|DESC],...</code>
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * The offset for the beginning of the results returned by the query. Can be null.
   *
   * @return the offset
   *         the offset, or starting point, of the query
   */
  public Integer getOffset() {
    return offset;
  }

  /**
   * The offset for the limit of the results returned by the query. Can be null.
   *
   * @return the limit for the query
   */
  public Integer getLimit() {
    return limit;
  }
}
