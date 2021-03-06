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

import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.netrelay.exception.NoSuchMapperException;
import de.braintags.netrelay.init.MappingDefinitions;
import de.braintags.netrelay.init.Settings;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.write.IWrite;
import de.braintags.vertx.jomnigate.dataaccess.write.IWriteResult;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import de.braintags.vertx.jomnigate.mapping.IMapperFactory;
import de.braintags.vertx.jomnigate.util.QueryHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * An abstract implementation for the different possible actions of {@link PersistenceController}
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractAction {
  protected static final String ERRORMESSAGE_RECNOTFOUND = "could not find record with ID %s";
  private PersistenceController persitenceController;
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AbstractAction.class);

  /**
   * 
   */
  public AbstractAction(PersistenceController persitenceController) {
    this.persitenceController = persitenceController;
  }

  protected PersistenceController getPersistenceController() {
    return persitenceController;
  }

  /**
   * Retrive the {@link IMapper} which is specified by the given mapperName.
   * 
   * @param mapperName
   *          the name of the mapper. This name mus exist as definition inside the {@link MappingDefinitions} of the
   *          {@link Settings}
   * @return a mapper from the internal {@link IMapperFactory}
   */
  @SuppressWarnings("rawtypes")
  protected IMapper<?> getMapper(String mapperName) {
    MappingDefinitions defs = persitenceController.getNetRelay().getSettings().getMappingDefinitions();
    Class mapperClass = defs.getMapperClass(mapperName);
    if (mapperClass == null) {
      throw new NoSuchMapperException(mapperName);
    }
    return persitenceController.getMapperFactory().getMapper(mapperClass);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void saveObjectInDatastore(Object ob, RoutingContext context, IMapper mapper,
      Handler<AsyncResult<Void>> handler) {
    IWrite write = getPersistenceController().getNetRelay().getDatastore().createWrite(mapper.getMapperClass());
    write.add(ob);
    write.save(res -> {
      AsyncResult<IWriteResult> result = (AsyncResult<IWriteResult>) res;
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        String entityName = mapper.getMapperClass().getSimpleName();
        LOGGER.info("adding new entity to context with key " + entityName);
        addToContext(context, entityName, ob);
        handler.handle(Future.succeededFuture());
      }
    });
  }

  /**
   * Add the object(s) into the {@link RoutingContext} by using the entityName as reference
   * 
   * @param context
   *          the context, where to place the object
   * @param entityName
   *          the key, by which it is placed
   * @param object
   *          the object to be stored; can be a single instance or a list
   */
  protected void addToContext(RoutingContext context, String entityName, Object object) {
    context.put(entityName, object);
  }

  /**
   * The sub module handles the request with the appropriate action
   * 
   * @param entityName
   *          the name of the entity defined by the request
   * @param context
   *          the context, where to get the request data from for instance
   * @param captureMap
   *          the resolved {@link CaptureMap}
   * @param handler
   *          the handler to be informed about the result
   */
  final void handle(String entityName, RoutingContext context, CaptureMap captureMap,
      Handler<AsyncResult<Void>> handler) {
    IMapper<?> mapper = getMapper(entityName);
    if (RecordContractor.isSubobjectDefinition(captureMap)) {
      handleSubobjectEntityDefinition(context, entityName, captureMap, mapper, handler);
    } else {
      handleRegularEntityDefinition(entityName, context, captureMap, mapper, handler);
    }
  }

  /**
   * If the entity defines a subobject to be handled, this method loads the main instance
   * 
   * @param map
   * @param mainMapper
   * @param handler
   */
  protected <T> void loadMainObject(CaptureMap map, IMapper<T> mainMapper, Handler<AsyncResult<T>> handler) {
    IQuery<T> query = getPersistenceController().getNetRelay().getDatastore().createQuery(mainMapper.getMapperClass());
    RecordContractor.extractId(mainMapper, map, query);
    QueryHelper.executeToFirstRecord(query, true, handler);
  }

  /**
   * The entity definition contains no reference onto a subobject, the main instance is handled directly
   * 
   * @param entityName
   *          the name of the entity defined by the request
   * @param context
   *          the context, where to get the request data from for instance
   * @param captureMap
   *          the resolved {@link CaptureMap}
   * @param mapper
   *          the mapper of the main instance to be used
   * @param handler
   *          the handler to be informed about the result
   */
  protected abstract void handleRegularEntityDefinition(String entityName, RoutingContext context,
      CaptureMap captureMap, IMapper<?> mapper, Handler<AsyncResult<Void>> handler);

  /**
   * The entity definition contains a reference onto a subobject inside the main object, which shall be handled
   * 
   * @param entityName
   *          the name of the entity defined by the request
   * @param context
   *          the context, where to get the request data from for instance
   * @param captureMap
   *          the resolved {@link CaptureMap}
   * @param mapper
   *          the mapper of the main instance to be used
   * @param handler
   *          the handler to be informed about the result
   */
  protected abstract void handleSubobjectEntityDefinition(RoutingContext context, String entityName,
      CaptureMap captureMap, IMapper<?> mapper, Handler<AsyncResult<Void>> handler);
}
