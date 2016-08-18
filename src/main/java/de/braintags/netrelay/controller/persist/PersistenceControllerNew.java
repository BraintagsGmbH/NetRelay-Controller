package de.braintags.netrelay.controller.persist;

import java.util.List;
import java.util.Properties;

import de.braintags.io.vertx.pojomapper.mapping.IMapperFactory;
import de.braintags.io.vertx.util.CounterObject;
import de.braintags.io.vertx.util.exception.InitException;
import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.controller.AbstractCaptureController;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.routing.CaptureCollection;
import de.braintags.netrelay.routing.CaptureDefinition;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class PersistenceControllerNew extends AbstractCaptureController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(PersistenceControllerNew.class);

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
  private void handlePersistence(RoutingContext context, List<CaptureMap> resolvedCaptureCollections,
      Handler<AsyncResult<Void>> handler) {
    CounterObject<Void> co = new CounterObject<>(resolvedCaptureCollections.size(), handler);
    for (CaptureMap map : resolvedCaptureCollections) {
      handleAction(context, map, result -> {
        if (result.failed()) {
          co.setThrowable(result.cause());
        } else {
          if (co.reduce()) {
            handler.handle(Future.succeededFuture());
          }
        }
      });
      if (co.isError()) {
        break;
      }
    }
  }

  private void handleAction(RoutingContext context, CaptureMap map, Handler<AsyncResult<Void>> handler) {
    AbstractAction action = resolveAction(map);
    String mapperName = RecordContractor.extractEntityName(map);
    LOGGER.info(String.format("handling action %s on mapper %s", action, mapperName));
    LOGGER.info("REQUEST-PARAMS: " + context.request().params().toString());
    LOGGER.info("FORM_PARAMS: " + context.request().formAttributes().toString());
    action.handle(mapperName, context, map, handler);
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
    String upDir = readProperty(PersistenceControllerNew.UPLOAD_DIRECTORY_PROP, null, true);
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
    def.setName(PersistenceControllerNew.class.getSimpleName());
    def.setBlocking(true);
    def.setController(PersistenceControllerNew.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/persistenceController/:entity/:action/read.html" });
    def.setCaptureCollection(createDefaultCaptureCollection());
    return def;
  }

  private static CaptureCollection[] createDefaultCaptureCollection() {
    CaptureDefinition[] defs = new CaptureDefinition[5];
    defs[0] = new CaptureDefinition("entity", PersistenceControllerNew.MAPPER_CAPTURE_KEY, false);
    defs[1] = new CaptureDefinition("action", PersistenceControllerNew.ACTION_CAPTURE_KEY, false);
    defs[2] = new CaptureDefinition("selectionSize", PersistenceControllerNew.SELECTION_SIZE_CAPTURE_KEY, false);
    defs[3] = new CaptureDefinition("selectionStart", PersistenceControllerNew.SELECTION_START_CAPTURE_KEY, false);
    defs[4] = new CaptureDefinition("orderBy", PersistenceControllerNew.ORDERBY_CAPTURE_KEY, false);

    CaptureCollection collection = new CaptureCollection();
    collection.setCaptureDefinitions(defs);
    CaptureCollection[] cc = new CaptureCollection[] { collection };
    return cc;
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
