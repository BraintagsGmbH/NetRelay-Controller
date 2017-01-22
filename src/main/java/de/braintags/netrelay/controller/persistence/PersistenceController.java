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
package de.braintags.netrelay.controller.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.braintags.vertx.jomnigate.mapping.IMapperFactory;
import de.braintags.vertx.util.exception.InitException;
import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.controller.AbstractCaptureController;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.routing.CaptureCollection;
import de.braintags.netrelay.routing.CaptureDefinition;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

/**
 * The PersistenceController is the instance, which translates the parameters and data of a request into a datastore
 * based action. A request like "http://localhost/article/detail?entity=article(ID:5)" will be interpreted by the
 * controller to fetch the article with the id 5 from the datastore and to store it inside the context, so that is can
 * be displayed by a template engine.
 * 
 * To understand the configuration, you should read the section "Capture Collection" inside the NetRelay documentation.
 * For more infos about how to secure data access, see {@link AuthenticationController}
 * 
 * Referencing subobjects <br/>
 * see package-info of the current package for infos how to reference subobjects per http request
 * 
 * 
 * <br/>
 * <br/>
 * possible paramters are:
 * <UL>
 * <LI>{@value #MAPPER_CAPTURE_KEY} - the name of the parameter, which specifies the mapper to be used<br/>
 * <LI>{@value #ACTION_CAPTURE_KEY} - possible actions are defined by {@link Action}<br/>
 * <LI>{@value #UPLOAD_DIRECTORY_PROP} - The name of the property, which defines the directory, where uploaded
 * files are transferred into. This can be "webroot/images/" for instance<br/>
 * <LI>{@value #UPLOAD_RELATIVE_PATH_PROP} - The name of the property, which defines the relative path for uploaded
 * files.
 * If the {@link #UPLOAD_DIRECTORY_PROP} is defined as "webroot/images/" for instance, then the relative path here
 * could be "images/"
 * <LI>{@value #SELECTION_SIZE_CAPTURE_KEY} - the name of the parameter, which defines the maximum size of a
 * resulting selection
 * <LI>{@value #SELECTION_START_CAPTURE_KEY} - the name of the parameter, which defines the position of the first
 * record in a selection
 * <LI>{@value #ORDERBY_CAPTURE_KEY} - the name of the parameter, which defines the sort arguments as comma
 * separated list in the form of fieldname asc / desc.
 * </UL>
 * Further parameters {@link AbstractCaptureController}
 * 
 * 
 * Example configuration:<br/>
 * This example configuration defines the Persistence-Controller to be active under the url /article/detail and
 * will
 * let run the above described actions.
 * "http://localhost/article/detail?entity=article(ID:5)" will load the article for display,
 * "http://localhost/article/detail?entity=article(ID:5)&action=DELETE" will delete this article from the datastore
 * <br/>
 * 
 * Example configuration: <br/>
 * 
 * <pre>
   {
      "name" : "PersistenceController",
      "routes" : [   "/article/detail" ],
      "controller" : "de.braintags.netrelay.controller.persistence.PersistenceController",
      "handlerProperties" : {
        "reroute" : "false",
        "cleanPath" : "true",
        "uploadDirectory" : "webroot/upload/",
        "uploadRelativePath" : "upload/"
      },
      "captureCollection" : [ {
        "captureDefinitions" : [ {
          "captureName" : "entity",
          "controllerKey" : "entity",
          "required" : false
        }, {
          "captureName" : "action",
          "controllerKey" : "action",
          "required" : false
        } ]
      } ]
    }
 * </pre>
 * 
 * @author Michael Remme
 * 
 * 
 */
public class PersistenceController extends AbstractCaptureController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(PersistenceController.class);

  /**
   * The name of the property, which defines the directory, where uploaded files are transferred into.
   * This can be "webroot/images/" for instance
   */
  public static final String UPLOAD_DIRECTORY_PROP = "uploadDirectory";

  /**
   * The name of the property, which defines the relative path for uploaded files. If the {@link #UPLOAD_DIRECTORY_PROP}
   * is defined as "webroot/images/" for instance, then the relative path here could be "images/"
   */
  public static final String UPLOAD_RELATIVE_PATH_PROP = "uploadRelativePath";

  /**
   * The name of the property in the request, which specifies the action
   */
  public static final String ACTION_CAPTURE_KEY = "action";

  /**
   * The name of a the property in the request, which specifies the mapper
   */
  public static final String MAPPER_CAPTURE_KEY = "mapper";

  /**
   * The name of the property in the request, which defines the number of records of a selection. This property is used
   * in case of action display, when a list of records shall be displayed
   */
  public static final String SELECTION_SIZE_CAPTURE_KEY = "selectionSize";

  /**
   * The name of the property in the request, which defines the start of a selection
   */
  public static final String SELECTION_START_CAPTURE_KEY = "selectionStart";

  /**
   * The name of the property in the request, which defines the fields to sort a selection.
   */
  public static final String ORDERBY_CAPTURE_KEY = "orderBy";

  private IMapperFactory mapperFactory;
  private DisplayAction displayAction;
  private InsertAction insertAction;
  private UpdateAction updateAction;
  private DeleteAction deleteAction;
  private NoneAction noneAction;

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractCaptureController#handle(io.vertx.ext.web.RoutingContext,
   * java.util.List, io.vertx.core.Handler)
   */
  @Override
  protected void handle(RoutingContext context, List<CaptureMap> resolvedCaptureCollections,
      Handler<AsyncResult<Void>> handler) {
    MemberUtil.getCurrentUser(context, getNetRelay(), result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        DataAuthorizator.checkAuthorization(context, result.result(), resolvedCaptureCollections, daRes -> {
          if (daRes.failed()) {
            handler.handle(Future.failedFuture(daRes.cause()));
          } else if (!daRes.result()) {
            context.fail(403);
          } else {
            handlePersistence(context, resolvedCaptureCollections, handler);
          }
        });
      }
    });
  }

  /**
   * @param context
   * @param resolvedCaptureCollections
   * @param handler
   */
  @SuppressWarnings("rawtypes")
  private void handlePersistence(RoutingContext context, List<CaptureMap> resolvedCaptureCollections,
      Handler<AsyncResult<Void>> handler) {
    if (resolvedCaptureCollections.isEmpty()) {
      handler.handle(Future.succeededFuture());
    } else {
      List<Future> fl = new ArrayList<>();
      for (CaptureMap map : resolvedCaptureCollections) {
        fl.add(handleAction(context, map));
      }
      CompositeFuture cf = CompositeFuture.all(fl);
      cf.setHandler(cfr -> {
        if (cfr.failed()) {
          handler.handle(Future.failedFuture(cfr.cause()));
        } else {
          handler.handle(Future.succeededFuture());
        }
      });
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Future handleAction(RoutingContext context, CaptureMap map) {
    AbstractAction action = resolveAction(map);
    String mapperName = RecordContractor.extractEntityName(map);
    LOGGER.info(String.format("handling action %s on mapper %s", action, mapperName));
    LOGGER.info("REQUEST-PARAMS: " + context.request().params().toString());
    LOGGER.info("FORM_PARAMS: " + context.request().formAttributes().toString());
    Future f = Future.future();
    action.handle(mapperName, context, map, f.completer());
    return f;
  }

  private AbstractAction resolveAction(CaptureMap map) {
    String actionKey = map.get(ACTION_CAPTURE_KEY);
    Action action = actionKey == null ? Action.DISPLAY : Action.valueOf(actionKey);
    LOGGER.info("action is " + action);
    switch (action) {
    case DISPLAY:
      return displayAction;
    case INSERT:
      return insertAction;
    case UPDATE:
      return updateAction;
    case DELETE:
      return deleteAction;
    case NONE:
      return noneAction;
    default:
      throw new UnsupportedOperationException("unknown action: " + action);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractCaptureController#internalInitProperties(java.util.Properties)
   */
  @Override
  protected void internalInitProperties(Properties properties) {
    mapperFactory = getNetRelay().getNetRelayMapperFactory();
    displayAction = new DisplayAction(this);
    insertAction = new InsertAction(this);
    updateAction = new UpdateAction(this);
    deleteAction = new DeleteAction(this);
    noneAction = new NoneAction(this);
    String upDir = readProperty(PersistenceController.UPLOAD_DIRECTORY_PROP, null, true);
    FileSystem fs = getVertx().fileSystem();
    if (!fs.existsBlocking(upDir)) {
      fs.mkdirsBlocking(upDir);
      if (!fs.existsBlocking(upDir)) {
        throw new InitException("could not create directory " + upDir);
      } else {
        LOGGER.info("Upload directory created: " + upDir);
      }
    }
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(PersistenceController.class.getSimpleName());
    def.setBlocking(true);
    def.setController(PersistenceController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/persistenceController/:entity/:action/read.html" });
    def.setCaptureCollection(createDefaultCaptureCollection());
    return def;
  }

  private static CaptureCollection[] createDefaultCaptureCollection() {
    CaptureDefinition[] defs = new CaptureDefinition[5];
    defs[0] = new CaptureDefinition("entity", PersistenceController.MAPPER_CAPTURE_KEY, false);
    defs[1] = new CaptureDefinition("action", PersistenceController.ACTION_CAPTURE_KEY, false);
    defs[2] = new CaptureDefinition("selectionSize", PersistenceController.SELECTION_SIZE_CAPTURE_KEY, false);
    defs[3] = new CaptureDefinition("selectionStart", PersistenceController.SELECTION_START_CAPTURE_KEY, false);
    defs[4] = new CaptureDefinition("orderBy", PersistenceController.ORDERBY_CAPTURE_KEY, false);

    CaptureCollection collection = new CaptureCollection();
    collection.setCaptureDefinitions(defs);
    return new CaptureCollection[] { collection };
  }

  /**
   * Get the default properties for an implementation
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(REROUTE_PROPERTY, "true");
    json.put(AUTO_CLEAN_PATH_PROPERTY, "true");
    json.put(UPLOAD_DIRECTORY_PROP, "webroot/images/");
    json.put(UPLOAD_RELATIVE_PATH_PROP, "images/");
    return json;
  }

  /**
   * @return the mapperFactory
   */
  public final IMapperFactory getMapperFactory() {
    return mapperFactory;
  }
}
