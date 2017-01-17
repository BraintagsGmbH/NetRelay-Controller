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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQueryResult;
import de.braintags.io.vertx.pojomapper.dataaccess.query.ISearchCondition;
import de.braintags.io.vertx.pojomapper.dataaccess.query.QueryOperator;
import de.braintags.io.vertx.pojomapper.dataaccess.query.impl.QueryOr;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.util.exception.InitException;
import de.braintags.io.vertx.util.exception.NoSuchFileException;
import de.braintags.io.vertx.util.file.FileSystemUtil;
import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.querypool.exceptions.DatastoreNotFoundException;
import de.braintags.netrelay.controller.querypool.exceptions.InvalidSyntaxException;
import de.braintags.netrelay.controller.querypool.exceptions.QueryPoolException;
import de.braintags.netrelay.controller.querypool.template.DynamicQuery;
import de.braintags.netrelay.controller.querypool.template.NativeQuery;
import de.braintags.netrelay.controller.querypool.template.QueryTemplate;
import de.braintags.netrelay.controller.querypool.template.dynamic.Condition;
import de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * The QueryPoolController executes queries from a pre-existing pool. <br>
 * The queries are located in the file system inside a folder specified in the configuration.<br>
 * The name of the query that should be executed is defined by the path of the request. It is translated to a file path,
 * with the file ending replaced by ".json". <br>
 * <br>
 * Example:<br>
 * If the configured query folder is "queries/"<br>
 * and request is "http://localhost/article/list"<br>
 * the controller will look for the file "queries/article/list.json"<br>
 * <br>
 * The queries will be loaded once at startup and are kept in cache for better performance. <br>
 * <br>
 * The possible configuration parameters are:<br>
 * <ul>
 * <li>{@value #QUERY_DIRECTORY_PROPERTY} - the name of the folder where the query JSON files are stored - default:
 * {@value #DEFAULT_QUERY_DIRECTORY}</li>
 * </ul>
 * <br>
 * Example Configuration:<br>
 *
 * <pre>
 * {
      "name" : "QueryPoolController",
      "routes" : [   "*" ],
      "controller" : "de.braintags.netrelay.controller.querypool.QueryPoolController",
      "handlerProperties" : {
        "queryDirectory": "queries/"
      }
    }
 * </pre>
 *
 * <br>
 * Copyright: Copyright (c) 13.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */
public class QueryPoolController extends AbstractController {

  public static final String QUERY_DIRECTORY_PROPERTY = "queryDirectory";
  private static final String DEFAULT_QUERY_DIRECTORY = "queries/";

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryPoolController.class);

  private Map<String, QueryTemplate> queries = new HashMap<>();

  /**
   * The operation to be executed for a query. Even with a native query, we still need to know what kind of result is to
   * be expected: a selection, a count, ... Because of this, the operation must be defined for all kinds of queries
   */
  public enum Operation {
    /**
     * A normal SELECT operation
     */
    SELECT {
      @Override
      public void executeQuery(IQuery<?> query, String mapper, RoutingContext context) {
        query.execute(qeResult -> {
          if (qeResult.failed()) {
            context.fail(qeResult.cause());
          } else {
            IQueryResult<?> queryResult = qeResult.result();
            if (queryResult.isEmpty()) {
              context.next();
            } else if (queryResult.size() == 1) {
              queryResult.iterator().next(nh -> {
                if (nh.failed())
                  context.fail(nh.cause());
                else {
                  context.put(mapper, nh.result());
                  context.next();
                }
              });
            } else {
              queryResult.toArray(array -> {
                if (array.failed()) {
                  context.fail(array.cause());
                } else {
                  context.put(mapper, Arrays.asList(array.result()));
                  context.next();
                }
              });
            }
          }
        });
      }
    },
    /**
     * An operation that returns only the number of results of the query
     */
    COUNT {
      @Override
      public void executeQuery(IQuery<?> query, String mapper, RoutingContext context) {
        query.executeCount(countResult -> {
          if (countResult.failed()) {
            context.fail(countResult.cause());
          } else {
            context.put(mapper, countResult.result().getCount());
            context.next();
          }
        });
      }
    };
    /**
     * Executes the configured operation for the given query and puts the result under the mapper name in the current
     * context
     *
     * @param query
     * @param mapper
     * @param context
     */
    public abstract void executeQuery(IQuery<?> query, String mapper, RoutingContext context);

    /**
     * Custom deserialization method to ignore the case of the JSON keys
     *
     * @param key
     *          the JSON key of the operation
     * @return
     */
    @JsonCreator
    public static Operation fromString(String key) {
      for (Operation operation : Operation.values()) {
        if (operation.name().equalsIgnoreCase(key)) {
          return operation;
        }
      }
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.netrelay.controller.AbstractController#handleController(io.vertx.ext.web.RoutingContext)
   */
  @Override
  protected void handleController(RoutingContext context) {
    String queryPath = context.normalisedPath();
    queryPath = FilenameUtils.removeExtension(queryPath).toLowerCase(Locale.US);
    if (queries.containsKey(queryPath)) {
      handleQuery(queries.get(queryPath), context);
    } else {
      context.next();
    }
  }

  /**
   * Executes the cached query for the current path. The result will be made available to the current context under the
   * mapper name configured in the query
   *
   * @param queryTemplate
   *          the cached query
   * @param context
   *          the current context
   */
  private void handleQuery(QueryTemplate queryTemplate, RoutingContext context) {
    String mapperName = queryTemplate.getMapper();
    Class<?> mapperClass = getNetRelay().getSettings().getMappingDefinitions().getMapperClass(mapperName);
    IQuery<?> query;
    try {
      query = parseQuery(queryTemplate, mapperClass, context);
    } catch (QueryPoolException e) {
      context.fail(e);
      return;
    }

    Operation operation = queryTemplate.getOperation();
    operation.executeQuery(query, mapperName, context);
  }

  /**
   * Parses the {@link QueryTemplate} to an {@link IQuery}
   *
   * @param queryTemplate
   *          the template created from the original JSON
   * @param mapperClass
   *          the class of the mapper configured in the query
   * @param context
   *          the current context
   * @return the parsed {@link IQuery}
   * @throws QueryPoolException
   *           if the query template can not be parsed, e.g. because of syntax exceptions
   */
  private IQuery<?> parseQuery(QueryTemplate queryTemplate, Class<?> mapperClass, RoutingContext context)
      throws QueryPoolException {
    try {
      IQuery<?> query = getNetRelay().getDatastore().createQuery(mapperClass);
      if (queryTemplate.getNativeQueries() != null) {
        parseNativeQuery(queryTemplate.getNativeQueries(), query, context);
      } else if (queryTemplate.getDynamicQuery() != null) {
        parseDynamicQuery(queryTemplate.getDynamicQuery(), query, context);
      }
      addOrderBy(queryTemplate, query, context);
      addLimit(queryTemplate, query, context);
      return query;
    } catch (QueryPoolException e) {
      // add the underlying JSON to the exception
      e.setQuery(queryTemplate.getSource());
      throw e;
    }
  }

  /**
   * Parses a dynamic query, which is independent of the current {@link IDataStore}
   *
   * @param dynamicQuery
   *          the dynamic query part of the {@link QueryTemplate}
   * @param mapperClass
   *          the class of the configured mapper for this query
   * @param context
   * @return the parsed {@link IQuery}
   * @throws QueryPoolException
   *           if there is an error during the parsing, e.g. invalid syntax
   */
  private IQuery<?> parseDynamicQuery(DynamicQuery dynamicQuery, IQuery<?> query, RoutingContext context)
      throws QueryPoolException {
    if (dynamicQuery.getRootQueryPart() != null) {
      ISearchCondition searchCondition = parseQueryParts(dynamicQuery.getRootQueryPart(), query, context);
      query.setSearchCondition(searchCondition);
    }
    return query;
  }

  /**
   * Convert the {@link DynamicQuery} of the {@link QueryTemplate} to an {@link ISearchCondition} of the {@link IQuery}.
   * Recursively converts query parts of containers
   *
   * @param queryPart
   *          the query part to convert
   * @param query
   *          the query, to create valid search condition parts
   * @param context
   * @return the converted {@link ISearchCondition}
   * @throws InvalidSyntaxException
   *           if the part is of an unknown type
   */
  private ISearchCondition parseQueryParts(QueryPart queryPart, IQuery<?> query, RoutingContext context)
      throws InvalidSyntaxException {
    if (queryPart.isAnd()) {
      List<QueryPart> queryParts = queryPart.getAnd();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = parseQueryParts(queryParts.get(i), query, context);
        searchConditions[i] = subQueryPart;
      }
      return query.and(searchConditions);
    } else if (queryPart.isOr()) {
      List<QueryPart> queryParts = queryPart.getOr();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = parseQueryParts(queryParts.get(i), query, context);
        searchConditions[i] = subQueryPart;
      }
      return new QueryOr(searchConditions);
    } else if (queryPart.isCondition()) {
      Condition condition = queryPart.getCondition();
      String field = condition.getField();
      QueryOperator operator = QueryOperator.translate(condition.getLogic());
      Object value = condition.getValue();
      if (value instanceof String) {
        String textValue = (String) value;
        value = replaceVariable(textValue, context);
      }
      return query.condition(field, operator, value);
    } else {
      throw new InvalidSyntaxException("Query part is neither and nor or nor condition");
    }
  }

  /**
   * Checks if the value is a variable in the form of "${location.variablename}". If yes, and the location is known,
   * returns the replaced value of the variable. Otherwise, simply returns the given value unchanged.
   *
   * @param value
   * @param context
   * @return
   */
  private Object replaceVariable(String value, RoutingContext context) {
    if (value.startsWith("${") && value.endsWith("}")) {
      String fullVariable = value.substring(2, value.length() - 1);
      int i = fullVariable.indexOf(':');

      String location = fullVariable.substring(0, i);
      String name = fullVariable.substring(i + 1);
      switch (location) {
      case "request":
        return context.request().getParam(name);
      case "mapper":
        int j = name.indexOf('.');
        if (j > 0) {
          String mapperName = name.substring(0, j);
          String path = name.substring(j + 1);
          Object mapperValue = context.get(mapperName);
          if (mapperValue instanceof Iterable) {
            List<Object> result = new ArrayList<>();
            Iterator<?> it = ((Iterable<?>) mapperValue).iterator();
            while (it.hasNext()) {
              Object resultValue = extractMapperValue(path, mapperValue);
              result.add(resultValue);
            }
            return result;
          } else {
            return extractMapperValue(path, mapperValue);
          }
        } else {
          throw new IllegalArgumentException(
              "Mapper variable must have at least 2 parts: the mapper name and the field: " + value);
        }
      case "context":
        return context.get(name);
      default:
        throw new IllegalArgumentException("Unknown variable location: " + location);
      }
    } else
      return value;
  }

  private Object extractMapperValue(String path, Object value) {
    IMapper<?> mapper = getNetRelay().getDatastore().getMapperFactory().getMapper(value.getClass());
    if (mapper == null)
      throw new IllegalArgumentException(
          "Can not extract value from a class that is not a mapper: " + value.getClass());

    String fieldName = path;
    int i = path.indexOf('.');
    if (i > 0) {
      fieldName = path.substring(0, i);
    }

    IField field = mapper.getField(fieldName);
    Object fieldValue = field.getPropertyAccessor().readData(value);
    if (fieldValue != null && i > 0) {
      return extractMapperValue(path.substring(i + 1), fieldValue);
    } else
      return fieldValue;
  }

  /**
   * Add the order by fields to the query
   *
   * @param queryTemplate
   *          the template with the orderBy configuration
   * @param query
   *          the query to which the order by fields should be added
   * @param context
   */
  private void addOrderBy(QueryTemplate queryTemplate, IQuery<?> query, RoutingContext context) {
    if (StringUtils.isNotBlank(queryTemplate.getOrderBy())) {
      String[] orderBys = queryTemplate.getOrderBy().split(",");
      for (int i = 0; i < orderBys.length; i++) {
        String orderBy = orderBys[i].trim();
        int j = orderBy.indexOf(' ');
        boolean ascending = true;
        if (j > 0) {
          String direction = orderBy.substring(j + 1);
          orderBy = orderBy.substring(0, j);
          direction = (String) replaceVariable(direction, context);
          if ("desc".equalsIgnoreCase(direction))
            ascending = false;
        }
        orderBy = (String) replaceVariable(orderBy, context);
        query.addSort(orderBy, ascending);
      }
    }
  }

  /**
   * @param queryTemplate
   * @param query
   * @param context
   */
  private void addLimit(QueryTemplate queryTemplate, IQuery<?> query, RoutingContext context) {
    if (StringUtils.isNotBlank(queryTemplate.getLimit())) {
      String limit = (String) replaceVariable(queryTemplate.getLimit(), context);
      query.setLimit(Integer.valueOf(limit));
    }
    if (StringUtils.isNotBlank(queryTemplate.getOffset())) {
      String offset = (String) replaceVariable(queryTemplate.getOffset(), context);
      query.setStart(Integer.valueOf(offset));
    }
  }

  /**
   * Parses a native query which is specific to the currently configured {@link IDataStore}
   *
   * @param nativeQueries
   *          a list of native queries, with one {@link IDataStore} and query per entry
   * @param mapperClass
   *          the mapper class of the query
   * @param context
   * @return the parsed query
   * @throws QueryPoolException
   *           if there is an error during parsing, or if the current datastore was not found in the list of native
   *           queries
   */
  private IQuery<?> parseNativeQuery(List<NativeQuery> nativeQueries, IQuery<?> query, RoutingContext context)
      throws QueryPoolException {
    IDataStore datastore = getNetRelay().getDatastore();
    for (NativeQuery nativeQuery : nativeQueries) {
      Class<?> datastoreClass = nativeQuery.getDatastore();
      if (datastoreClass.equals(datastore.getClass())) {
        query.setNativeCommand(nativeQuery.getQuery());
        return query;
      }
    }
    throw new DatastoreNotFoundException("Query has a native block, but the current datastore ('"
        + datastore.getClass().getName() + "') is not defined");
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    String queryDir = readProperty(QUERY_DIRECTORY_PROPERTY, DEFAULT_QUERY_DIRECTORY, false);
    FileSystem fs = getVertx().fileSystem();
    if (!fs.existsBlocking(queryDir)) {
      fs.mkdirsBlocking(queryDir);
      if (!fs.existsBlocking(queryDir)) {
        throw new InitException("could not create directory " + queryDir);
      } else {
        LOGGER.info("Query directory created: " + queryDir);
      }
    } else {
      try {
        loadQueries(queryDir);
      } catch (NoSuchFileException e) {
        throw new InitException("could not read directory " + queryDir, e);
      }
    }
  }

  /**
   * Read all query JSON files inside the given directory and add them to the query cache
   *
   * @param directory
   *          the directory of the file system where the queries are stored
   * @throws NoSuchFileException
   *           if the given directory doesn't exist
   */
  private void loadQueries(String directory) throws NoSuchFileException {
    ObjectMapper om = new ObjectMapper();
    List<String> files = FileSystemUtil.getChildren(getVertx(), directory, true, null);
    for (String file : files) {
      getVertx().fileSystem().readFile(file, readFileHandler -> {
        if (readFileHandler.failed()) {
          throw new InitException("could not read query file: " + file);
        } else {
          String queryName = buildQueryName(file, directory);
          QueryTemplate template;
          Buffer fileResult = readFileHandler.result();
          try {
            template = om.readValue(fileResult.toString(), QueryTemplate.class);
          } catch (IOException e) {
            throw new InitException("Invalid query template file: " + file, e);
          }
          template.setSource(fileResult.toJsonObject());
          queries.put(queryName, template);

        }
      });
    }
  }

  /**
   * Creates the name under which this query will be found in the cache map. This method removes the file ending,
   * ensures it starts with a '/', and converts the name to lower case
   *
   * @param file
   * @param directory
   * @return
   */
  private String buildQueryName(String file, String directory) {
    StringBuilder query = new StringBuilder();
    query.append(file.substring(directory.length()));
    if (query.charAt(0) != '/')
      query.insert(0, '/');
    int i = query.lastIndexOf(".");
    if (i > 0)
      query.delete(i, query.length());
    return query.toString().toLowerCase(Locale.US);
  }

  /**
   * Creates a default definition for a {@link QueryPoolController}
   *
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(QueryPoolController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(QueryPoolController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "*" });
    return def;
  }

  /**
   * Get the default properties for an implementation of {@link QueryPoolController}
   *
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(QUERY_DIRECTORY_PROPERTY, DEFAULT_QUERY_DIRECTORY);
    return json;
  }
}
