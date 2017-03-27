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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.querypool.exceptions.DatastoreNotFoundException;
import de.braintags.netrelay.controller.querypool.exceptions.IndexedFieldNotFoundException;
import de.braintags.netrelay.controller.querypool.exceptions.InvalidSyntaxException;
import de.braintags.netrelay.controller.querypool.exceptions.QueryPoolException;
import de.braintags.netrelay.controller.querypool.template.DynamicQuery;
import de.braintags.netrelay.controller.querypool.template.NativeQuery;
import de.braintags.netrelay.controller.querypool.template.QueryTemplate;
import de.braintags.netrelay.controller.querypool.template.dynamic.Condition;
import de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.vertx.jomnigate.IDataStore;
import de.braintags.vertx.jomnigate.dataaccess.query.IFieldValueResolver;
import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.IQueryResult;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.dataaccess.query.QueryOperator;
import de.braintags.vertx.jomnigate.dataaccess.query.impl.QueryOr;
import de.braintags.vertx.util.exception.InitException;
import de.braintags.vertx.util.exception.NoSuchFileException;
import de.braintags.vertx.util.file.FileSystemUtil;
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
 * The offset and limit of the query are defined by the first non-null value in the following places:
 * <ol>
 * <li>request parameters {@value #OFFSET_PARAMETER_NAME} and {@value #LIMIT_PARAMETER_NAME}</li>
 * <li>the entries "limit" and "offset" in the JSON file</li>
 * <li>the datastore default values</li>
 * </ol>
 * <br>
 * <br>
 * The queries will be loaded once at startup and are kept in cache for better performance. <br>
 * <br>
 * The possible configuration parameters are:<br>
 * <ul>
 * <li>{@value #QUERY_DIRECTORY_PROPERTY} - the name of the folder where the query JSON files are stored - default:
 * {@value #DEFAULT_QUERY_DIRECTORY}</li>
 * </ul>
 * Example Configuration:<br>
 *
 * <pre>
 * {
 *  "name" : "QueryPoolController",
 *  "routes" : [ "*" ],
 *  "controller" : "de.braintags.netrelay.controller.querypool.QueryPoolController",
 *  "handlerProperties" : {
 *    "queryDirectory": "queries/"
 *  }
 * }
 * </pre>
 *
 * <br>
 * Copyright: Copyright (c) 13.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */
public class QueryPoolController extends AbstractController {

  /**
   * Name of the request parameter that sets the query offset
   */
  private static final String OFFSET_PARAMETER_NAME = "qoffset";
  /**
   * Name of the request parameter that sets the query limit
   */
  private static final String LIMIT_PARAMETER_NAME = "qlimit";

  /**
   * Name of the property key that defines the root directory where the query template JSON files are
   */
  public static final String QUERY_DIRECTORY_PROPERTY = "queryDirectory";
  /**
   * Default directory name where query template JSON files are
   */
  private static final String DEFAULT_QUERY_DIRECTORY = "queries/";

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryPoolController.class);

  /**
   * Cache for the built queries. A simple map should suffice, since the are only build and stored during initialization
   * and shouldn't change afterwards
   */
  private Map<String, CacheEntry> queries = new HashMap<>();

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
      public void executeQuery(CacheEntry entry, IFieldValueResolver resolver, RoutingContext context) {
        String limitValue = context.request().getParam(LIMIT_PARAMETER_NAME);
        int limit = StringUtils.isNotBlank(limitValue) ? Integer.valueOf(limitValue) : entry.defaultLimit;
        String offsetValue = context.request().getParam(OFFSET_PARAMETER_NAME);
        int offset = StringUtils.isNotBlank(offsetValue) ? Integer.valueOf(offsetValue) : entry.defaultOffset;
        entry.query.execute(resolver, limit, offset, qeResult -> {
          if (qeResult.failed()) {
            context.fail(qeResult.cause());
          } else {
            IQueryResult<?> queryResult = qeResult.result();
            if (queryResult.isEmpty()) {
              context.next();
            } else if (queryResult.size() == 1) {
              queryResult.iterator().next(nh -> {
                if (nh.failed()) {
                  context.fail(nh.cause());
                } else {
                  context.put(entry.destination, nh.result());
                  context.next();
                }
              });
            } else {
              queryResult.toArray(array -> {
                if (array.failed()) {
                  context.fail(array.cause());
                } else {
                  context.put(entry.destination, Arrays.asList(array.result()));
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
      public void executeQuery(CacheEntry entry, IFieldValueResolver resolver, RoutingContext context) {
        entry.query.executeCount(resolver, countResult -> {
          if (countResult.failed()) {
            context.fail(countResult.cause());
          } else {
            context.put(entry.destination, countResult.result().getCount());
            context.next();
          }
        });
      }
    };
    /**
     * Executes the configured operation for the given query and puts the result under the destination name in the
     * current context
     *
     * @param entry
     *          contains the query, and all information needed to execute it (i.e. destination, limit, offset)
     * @param resolver
     *          the variable resolver needed to translate field condition variables to their actual value
     * @param context
     *          the current routing context
     */
    public abstract void executeQuery(CacheEntry entry, IFieldValueResolver resolver, RoutingContext context);

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
    CacheEntry entry = queries.get(queryPath);
    if (entry != null) {
      handleQuery(entry, context);
    } else {
      context.next();
    }
  }

  /**
   * Executes the cached query. The result will be made available to the current context under the destination
   * configured in the cache entry.
   *
   * @param entry
   *          the cache entry containing the query and all needed information
   * @param context
   *          the current context
   */
  private void handleQuery(CacheEntry entry, RoutingContext context) {
    IFieldValueResolver resolver = new ContextFieldValueResolver(context, getNetRelay());
    Operation operation = entry.operation;
    operation.executeQuery(entry, resolver, context);
  }

  /**
   * Parses the {@link QueryTemplate} to an {@link IQuery}
   *
   * @param queryTemplate
   *          the template created from the original JSON
   * @return the parsed {@link IQuery}
   * @throws QueryPoolException
   *           if the query template can not be parsed, e.g. because of syntax exceptions
   */
  private IQuery<?> parseQuery(QueryTemplate queryTemplate) throws QueryPoolException {
    Class<?> mapperClass = getNetRelay().getSettings().getMappingDefinitions()
        .getMapperClass(queryTemplate.getMapper());
    try {
      IQuery<?> query = getNetRelay().getDatastore().createQuery(mapperClass);
      if (queryTemplate.getNativeQueries() != null) {
        parseNativeQuery(queryTemplate.getNativeQueries(), query);
      } else if (queryTemplate.getDynamicQuery() != null) {
        parseDynamicQuery(queryTemplate.getDynamicQuery(), query);
      }
      if (StringUtils.isNotBlank(queryTemplate.getOrderBy())) {
        addOrderBy(queryTemplate, query);
      }
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
   * @param query
   *          the query where the content of the dynamic content will be added to
   * @throws QueryPoolException
   *           if there is an error during the parsing, e.g. invalid syntax
   */
  private void parseDynamicQuery(DynamicQuery dynamicQuery, IQuery<?> query) throws QueryPoolException {
    if (dynamicQuery.getRootQueryPart() != null) {
      ISearchCondition searchCondition = parseQueryParts(dynamicQuery.getRootQueryPart(), query);
      query.setSearchCondition(searchCondition);
    }
  }

  /**
   * Recursively convert the {@link DynamicQuery} of the {@link QueryTemplate} to an {@link ISearchCondition} of the
   * {@link IQuery}.
   *
   * @param queryPart
   *          the query part to convert
   * @param query
   *          the query, to create valid search condition parts
   * @return the converted {@link ISearchCondition}
   * @throws InvalidSyntaxException
   *           if the part is of an unknown type
   */
  private ISearchCondition parseQueryParts(QueryPart queryPart, IQuery<?> query) throws QueryPoolException {
    if (queryPart.isAnd()) {
      List<QueryPart> queryParts = queryPart.getAnd();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = parseQueryParts(queryParts.get(i), query);
        searchConditions[i] = subQueryPart;
      }
      return ISearchCondition.and(searchConditions);
    } else if (queryPart.isOr()) {
      List<QueryPart> queryParts = queryPart.getOr();
      ISearchCondition[] searchConditions = new ISearchCondition[queryParts.size()];
      for (int i = 0; i < queryParts.size(); i++) {
        ISearchCondition subQueryPart = parseQueryParts(queryParts.get(i), query);
        searchConditions[i] = subQueryPart;
      }
      return new QueryOr(searchConditions);
    } else if (queryPart.isCondition()) {
      Condition condition = queryPart.getCondition();
      String field = condition.getField();
      QueryOperator operator = condition.getLogic();
      Object value = condition.getValue();
      IIndexedField indexedField;
      try {
        indexedField = IIndexedField.getIndexedField(field, query.getMapperClass());
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new IndexedFieldNotFoundException(
            "No IndexedField found for name '" + field + "' in class '" + query.getMapperClass() + "'", e);
      }
      return ISearchCondition.condition(indexedField, operator, value);
    } else {
      throw new InvalidSyntaxException("Query part is neither 'and' nor 'or' nor 'condition'");
    }
  }

  /**
   * Add the order by fields to the query
   *
   * @param queryTemplate
   *          the template with the orderBy configuration
   * @param query
   *          the query to which the order by fields should be added
   */
  private void addOrderBy(QueryTemplate queryTemplate, IQuery<?> query) {
    String[] orderBys = queryTemplate.getOrderBy().split(",");
    for (String orderBy2 : orderBys) {
      String[] orderBy = orderBy2.trim().split("\\s+");
      boolean ascending = true;
      if (orderBy.length > 1) {
        String direction = orderBy[1];
        if ("desc".equalsIgnoreCase(direction)) {
          ascending = false;
        }
      }
      query.addSort(orderBy[0], ascending);
    }
  }

  /**
   * Parses a native query which is specific to the currently configured {@link IDataStore}
   *
   * @param nativeQueries
   *          a list of native queries, with one {@link IDataStore} and query per entry
   * @param query
   *          the query to which the natiev query should be set to
   * @throws DatastoreNotFoundException
   *           if the current datastore was not found in the list of native queries
   */
  private void parseNativeQuery(List<NativeQuery> nativeQueries, IQuery<?> query) throws DatastoreNotFoundException {
    IDataStore datastore = getNetRelay().getDatastore();
    boolean found = false;
    for (NativeQuery nativeQuery : nativeQueries) {
      Class<?> datastoreClass = nativeQuery.getDatastore();
      if (datastoreClass.equals(datastore.getClass())) {
        query.setNativeCommand(nativeQuery.getQuery());
        found = true;
        break;
      }
    }
    if (!found) {
      throw new DatastoreNotFoundException("Query has a native block, but the current datastore ('"
          + datastore.getClass().getName() + "') is not defined");
    }
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
      Buffer fileResult = getVertx().fileSystem().readFileBlocking(file);
      String queryName = buildQueryName(file, directory);
      QueryTemplate template;
      try {
        template = om.readValue(fileResult.toString(), QueryTemplate.class);
      } catch (IOException e) {
        throw new InitException("Invalid query template file: " + file, e);
      }
      template.setSource(fileResult.toJsonObject());

      IQuery<?> query;
      try {
        query = parseQuery(template);
      } catch (QueryPoolException e) {
        throw new InitException("Could not parse query template: " + template.getSource(), e);
      }

      int defaultLimit = template.getLimit() != null ? template.getLimit()
          : getNetRelay().getDatastore().getDefaultQueryLimit();
      int defaultOffset = template.getOffset() != null ? template.getOffset() : 0;

      CacheEntry cacheEntry = new CacheEntry(query, template.getOperation(), defaultLimit, defaultOffset,
          template.getMapper());
      queries.put(queryName, cacheEntry);
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
    if (query.charAt(0) != '/') {
      query.insert(0, '/');
    }
    int i = query.lastIndexOf(".");
    if (i > 0) {
      query.delete(i, query.length());
    }
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

  /**
   * Small POJO to hold all needed information for a query in the cache
   */
  private static class CacheEntry {
    private final IQuery<?> query;
    private final int defaultLimit;
    private final int defaultOffset;
    private final Operation operation;
    private final String destination;

    public CacheEntry(IQuery<?> query, Operation operation, int limit, int offset, String destination) {
      this.query = query;
      this.operation = operation;
      this.defaultLimit = limit;
      this.defaultOffset = offset;
      this.destination = destination;
    }

  }
}
