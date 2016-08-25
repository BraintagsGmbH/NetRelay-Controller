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

import java.util.List;
import java.util.Map;

import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.util.exception.ParameterRequiredException;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.netrelay.mapping.NetRelayStoreObjectFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class UpdateAction extends InsertAction {

  /**
   * @param persitenceController
   */
  public UpdateAction(PersistenceController persitenceController) {
    super(persitenceController);
  }

  /**
   * This method fetches the required subobject, fills it with the transported properties and saves the parent instance
   * 
   * @param context
   * @param entityName
   * @param captureMap
   * @param mapper
   * @param mainObject
   *          this object will be saved after modification of the subobject
   * @param handler
   */
  @Override
  protected void handleSubObject(RoutingContext context, String entityName, CaptureMap captureMap, IMapper mapper,
      Object mainObject, Handler<AsyncResult<Void>> handler) {
    InsertParameter ip = RecordContractor.resolveUpdateParameter(mapper.getMapperFactory(), mainObject, captureMap);
    String subEntityName = ip.getFieldPath();
    Map<String, String> params = extractProperties(subEntityName, captureMap, context, ip.getSubObjectMapper());
    handleFileUploads(subEntityName, context, params);
    NetRelayStoreObjectFactory nsf = (NetRelayStoreObjectFactory) getPersistenceController().getMapperFactory()
        .getStoreObjectFactory();
    nsf.createStoreObject(params, ip.getUpdateObject(), ip.getSubObjectMapper(), result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        saveObjectInDatastore(mainObject, context, mapper, handler);
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.persistence.InsertAction#extractProperties(java.lang.String,
   * io.vertx.ext.web.RoutingContext)
   */
  @Override
  protected Map<String, String> extractProperties(String entityName, CaptureMap captureMap, RoutingContext context,
      IMapper mapper) {
    Map<String, String> map = super.extractProperties(entityName, captureMap, context, mapper);
    List<String[]> ids = RecordContractor.extractIds(mapper, captureMap);
    boolean idFieldFound = false;
    for (String[] id : ids) {
      if (id[0].equalsIgnoreCase(mapper.getIdField().getName())) {
        idFieldFound = true;
        map.put(mapper.getIdField().getName().toLowerCase(), id[1]);
      }
    }
    if (!idFieldFound) {
      throw new ParameterRequiredException("The update action needs the id field in the record reference");
    }
    return map;
  }

}
