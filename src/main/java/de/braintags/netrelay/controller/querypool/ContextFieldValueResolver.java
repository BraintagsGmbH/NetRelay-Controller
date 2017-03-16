/*
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
package de.braintags.netrelay.controller.querypool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.braintags.netrelay.NetRelay;
import de.braintags.vertx.jomnigate.dataaccess.query.IFieldValueResolver;
import de.braintags.vertx.jomnigate.dataaccess.query.exception.VariableSyntaxException;
import de.braintags.vertx.jomnigate.mapping.IProperty;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import io.vertx.ext.web.RoutingContext;

/**
 * Resolves variables of the value inside a field condition.
 * The variable must be build with the following syntax:
 * <p>
 * <code>${&lt;location&gt;:&lt;name&gt;}</code>
 * </p>
 * For example:
 * <p>
 * <code>${request:articlekey}</code>
 * </p>
 * Current possibilities for the location are:
 * <ul>
 * <li><code>{@value #LOCATION_REQUEST}</code>: fetch the value from a request parameter value of the current
 * request</li>
 * <li><code>{@value #LOCATION_MAPPER}</code>: fetch the value from one or more records available in the current
 * context</li>
 * <li><code>{@value #LOCATION_CONTEXT}</code>: fetch the value from the data map of the current routing context</li>
 * </ul>
 * For requests and context, the value is simply looked up by using the whole name part as key.
 *
 * For mappers, the name must be a field path in a dot notation, e.g. "customer.id". The path can be multiple levels
 * deep, e.g. "customer.address.street". Also, if there are multiple records for "customer", every value for the given
 * field will be combined to a list and returned as result.
 *
 * @author sschmitt
 *
 */
public class ContextFieldValueResolver implements IFieldValueResolver {

  /**
   * Identifier for variable values that should be fetched from the current rounting context
   */
  private static final String LOCATION_CONTEXT = "context";
  /**
   * Identifier for variable values that should be fetched from a value of an available mapper record
   */
  private static final String LOCATION_MAPPER = "mapper";
  /**
   * Identifier for variable values that should be fetched from a request parameter
   */
  private static final String LOCATION_REQUEST = "request";

  private static final String[] LOCATIONS = { LOCATION_CONTEXT, LOCATION_MAPPER, LOCATION_REQUEST };

  private RoutingContext context;
  private NetRelay netRelay;

  public ContextFieldValueResolver(RoutingContext context, NetRelay netRelay) {
    this.context = context;
    this.netRelay = netRelay;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.vertx.jomnigate.dataaccess.query.IFieldValueResolver#resolve(java.lang.String)
   */
  @Override
  public Object resolve(String variable) throws VariableSyntaxException {
    int i = variable.indexOf(':');

    String location = variable.substring(0, i);
    String variableName = variable.substring(i + 1);
    switch (location) {
    case LOCATION_REQUEST:
      return getRequestParameterValue(variableName);
    case LOCATION_MAPPER:
      return getMapperValue(variableName);
    case LOCATION_CONTEXT:
      return getContextValue(variableName);
    default:
      throw new VariableSyntaxException(
          "Unknown variable location: '" + location + "'. Possible locations are: " + StringUtils.join(LOCATIONS, ','));
    }
  }

  /**
   * Fetch the value from the data map of the current routing context
   *
   * @param variableName
   *          the name of the variable
   * @return the value found in the current context
   */
  private Object getContextValue(String variableName) {
    return context.get(variableName);
  }

  /**
   * Fetch the value from the field of one or more mapper records that are currently available. If there is more than
   * one record, the list will be looped and all the field values of the given field will be combined in a list.
   * The variable must be in dot notation and can be multiple fields deep, e.g. 'customer.address.street'
   *
   * @param variableName
   *          the name of the variable
   * @return the field value(s) of one or more mappers found with the given variable name
   * @throws VariableSyntaxException
   */
  private Object getMapperValue(String variableName) throws VariableSyntaxException {
    int i = variableName.indexOf('.');
    if (i > 0) {
      String mapperName = variableName.substring(0, i);
      String path = variableName.substring(i + 1);
      Object mapperValue = context.get(mapperName);
      if (mapperValue instanceof Iterable) {
        List<Object> result = new ArrayList<>();
        Iterator<?> it = ((Iterable<?>) mapperValue).iterator();
        while (it.hasNext()) {
          Object resultValue = extractMapperValue(path, mapperValue);
          if (resultValue != null)
            result.add(resultValue);
        }
        return result;
      } else {
        return extractMapperValue(path, mapperValue);
      }
    } else {
      throw new VariableSyntaxException(
          "Mapper variable must have at least 2 parts: the mapper name and the field: " + variableName);
    }
  }

  /**
   * Recursive method to loop through the path
   *
   * @param path
   *          the current path in dot notation, e.g. 'customer.address.street'
   * @param record
   *          the record from which to extract the value
   * @return the value of the given record denoted by the given path
   */
  private Object extractMapperValue(String path, Object record) {
    IMapper<?> mapper = netRelay.getDatastore().getMapperFactory().getMapper(record.getClass());
    if (mapper == null)
      throw new IllegalArgumentException(
          "Can not extract value from a class that is not a mapper: " + record.getClass());

    String fieldName = path;
    int i = path.indexOf('.');
    if (i > 0) {
      fieldName = path.substring(0, i);
    }

    IProperty field = mapper.getField(fieldName);
    Object fieldValue = field.getPropertyAccessor().readData(record);
    if (fieldValue != null && i > 0) {
      return extractMapperValue(path.substring(i + 1), fieldValue);
    } else
      return fieldValue;
  }

  /**
   * Fetch the value from a request parameter given in the current HTTP request
   *
   * @param name
   *          the name of the variable
   * @return the value of the request parameter
   */
  private Object getRequestParameterValue(String name) {
    return context.request().getParam(name);
  }

}
