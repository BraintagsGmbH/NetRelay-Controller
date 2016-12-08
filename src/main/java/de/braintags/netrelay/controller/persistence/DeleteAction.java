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

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.delete.IDelete;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Action is used to delete single records
 * 
 * @author Michael Remme
 * 
 */
public class DeleteAction extends AbstractAction {

  /**
   * @param persitenceController
   */
  public DeleteAction(PersistenceController persitenceController) {
    super(persitenceController);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected void handleRegularEntityDefinition(String entityName, RoutingContext context, CaptureMap captureMap,
      IMapper<?> mapper, Handler<AsyncResult<Void>> handler) {
    IDataStore datastore = getPersistenceController().getNetRelay().getDatastore();
    IDelete<?> delete = datastore.createDelete(mapper.getMapperClass());
    IQuery query = getPersistenceController().getNetRelay().getDatastore().createQuery(mapper.getMapperClass());
    RecordContractor.extractId(mapper, captureMap, query);
    delete.setQuery(query);
    delete.delete(result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.persistence.AbstractAction#handleSubobjectEntityDefinition(io.vertx.ext.web.
   * RoutingContext, java.lang.String, de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap,
   * de.braintags.io.vertx.pojomapper.mapping.IMapper, io.vertx.core.Handler)
   */
  @Override
  protected void handleSubobjectEntityDefinition(RoutingContext context, String entityName, CaptureMap captureMap,
      IMapper<?> mapper, Handler<AsyncResult<Void>> handler) {
    loadMainObject(captureMap, mapper, mor -> {
      if (mor.failed()) {
        handler.handle(Future.failedFuture(mor.cause()));
      } else {
        deleteSubObject(context, captureMap, mapper, mor.result(), handler);
      }
    });
  }

  /**
   * @param context
   * @param entityName
   * @param captureMap
   * @param mapper
   * @param mainObject
   *          this object will be saved after modification of the subobject
   * @param handler
   */
  private void deleteSubObject(RoutingContext context, CaptureMap captureMap, IMapper mapper, Object mainObject,
      Handler<AsyncResult<Void>> handler) {
    DeleteParameter dp = RecordContractor.resolveDeleteParameter(mapper.getMapperFactory(), mainObject, captureMap);
    if (dp.getParentCollection().remove(dp.getDeleteObject())) {
      saveObjectInDatastore(mainObject, context, mapper, handler);
    } else {
      handler.handle(Future.failedFuture(
          new IllegalArgumentException("Could not delete an object from a list: " + dp.getDeleteObject().toString())));
    }
  }

}
