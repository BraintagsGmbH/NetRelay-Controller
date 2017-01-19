/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mapping.IStoreObject;
import de.braintags.io.vertx.pojomapper.mapping.IStoreObjectFactory;
import de.braintags.io.vertx.util.HttpContentType;
import de.braintags.io.vertx.util.exception.ParameterRequiredException;
import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.api.DataTableLinkDescriptor.ColDef;
import de.braintags.netrelay.mapping.NetRelayMapperFactory;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A controller, which generates the input for a jquery datatable
 * <br/>
 * Config-Parameter:<br/>
 * None
 * <br>
 *
 * Request-Parameter:<br/>
 * <UL>
 * <LI>{@value #MAPPER_KEY} - the name of the property, which specifies the mapper to be used
 * </UL>
 * <br/>
 *
 * Result-Parameter:<br/>
 * sends the result as Json in the required form
 * <br/>
 *
 * <pre>

     {
      "name" : "DataTableController",
      "routes" : [ "/api/datatables" ],
      "controller" : "de.braintags.netrelay.controller.api.DataTablesController",
      "handlerProperties" : {
        "cacheEnabled" : "false"
      }
    }
 *
 * </pre>
 *
 *
 *
 * @author Michael Remme
 *
 */
public class DataTablesController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(DataTablesController.class);

  /**
   * The name of a the property in the request, which specifies the mapper
   */
  public static final String MAPPER_KEY = "mapper";

  private NetRelayMapperFactory mapperFactory;

  /*
   * (non-Javadoc)
   *
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    String mapperName = context.request().getParam(MAPPER_KEY);
    if (mapperName == null) {
      context.fail(new ParameterRequiredException(MAPPER_KEY));
    } else {
      Class mapperClass = getNetRelay().getSettings().getMappingDefinitions().getMapperClass(mapperName);
      Objects.requireNonNull(mapperClass,
          "Could not determine mapper class for " + mapperName + ". Check the configuration");
      DataTableLinkDescriptor descr = new DataTableLinkDescriptor(mapperClass, context);
      handleQuery(context, descr);
    }
  }

  /**
   * @param context
   * @param descr
   */
  private void handleQuery(RoutingContext context, DataTableLinkDescriptor descr) {
    IQuery<?> tableQuery = descr.toRecordsInTableQuery(getNetRelay().getDatastore());
    tableQuery.executeCount(tqResult -> {
      if (tqResult.failed()) {
        context.fail(tqResult.cause());
      } else {
        long tableCount = tqResult.result().getCount();
        descr.toQuery(getNetRelay().getDatastore(), getNetRelay().getNetRelayMapperFactory(), qr -> {
          if (qr.failed()) {
            context.fail(qr.cause());
          } else {
            IQuery<?> query = qr.result();
            execute(query, descr, tableCount, result -> {
              if (result.failed()) {
                context.fail(result.cause());
              } else {
                HttpServerResponse response = context.response();
                response.putHeader("content-type", HttpContentType.JSON_UTF8.toString())
                    .end(result.result().encodePrettily());
              }
            });
          }
        });
      }
    });
  }

  private void execute(IQuery<?> query, DataTableLinkDescriptor descr, long tableCount,
      Handler<AsyncResult<JsonObject>> handler) {
    query.execute(null, descr.getDisplayLength(), descr.getDisplayStart(), qr -> {
      if (qr.failed()) {
        handler.handle(Future.failedFuture(qr.cause()));
      } else {
        LOGGER.info(qr.result().getOriginalQuery());
        long totalResult = qr.result().getCompleteResult();
        qr.result().toArray(result -> {
          if (result.failed()) {
            handler.handle(Future.failedFuture(result.cause()));
          } else {
            Object[] selection = result.result();
            LOGGER.info("SELECTION SIZE: " + selection.length);
            if (selection.length == 0) {
              handler.handle(Future.succeededFuture(createJsonObject(query.getMapper(),
                  new ArrayList<IStoreObject<?, ?>>(), descr, totalResult, tableCount)));
            } else {
              IStoreObjectFactory<?> sf = mapperFactory.getStoreObjectFactory();
              sf.createStoreObjects(query.getMapper(), Arrays.asList(selection), str -> {
                if (str.failed()) {
                  handler.handle(Future.failedFuture(result.cause()));
                } else {
                  List tmpSel = str.result();
                  JsonObject jo = createJsonObject(query.getMapper(), tmpSel, descr, totalResult, tableCount);
                  handler.handle(Future.succeededFuture(jo));
                }
              });
            }
          }
        });
      }
    });
  }

  private JsonObject createJsonObject(IMapper mapper, List<IStoreObject<?, ?>> selection, DataTableLinkDescriptor descr,
      long completeCount, long tableCount) {
    LOGGER.info("tableCount: " + tableCount + ", completeCount: " + completeCount);
    JsonObject json = new JsonObject();
    json.put("recordsTotal", tableCount);
    json.put("recordsFiltered", completeCount);
    JsonArray resArray = new JsonArray();
    json.put("data", resArray);
    for (IStoreObject<?, ?> ob : selection) {
      resArray.add(handleObject(mapper, ob, descr));
    }
    return json;
  }

  private JsonArray handleObject(IMapper mapper, IStoreObject<?, ?> sto, DataTableLinkDescriptor descr) {
    JsonArray json = new JsonArray();
    for (ColDef colDef : descr.getColumns()) {
      if (colDef != null && colDef.name != null && colDef.name.hashCode() != 0) {
        IField field = mapper.getField(colDef.name);
        Objects.requireNonNull(field, "Could not find defined field for '" + colDef.name + "'");
        Object value = sto.get(field);
        json.add(value == null ? "" : value);
      } else {
        json.add("");
      }
    }
    return json;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    mapperFactory = new NetRelayMapperFactory(getNetRelay());
  }

  /**
   * Creates a default definition for the current instance
   *
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setActive(false);
    def.setName(DataTablesController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(DataTablesController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] {});
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   *
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    return json;
  }

}
