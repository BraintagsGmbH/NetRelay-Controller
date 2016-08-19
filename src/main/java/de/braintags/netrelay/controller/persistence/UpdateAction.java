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
