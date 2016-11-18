/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2016 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.filemanager.elfinder.command.impl;

import java.util.List;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class SizeCommand extends AbstractCommand<List<ITarget>> {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<List<ITarget>>> handler) {
    final List<String> targets = efContext.getRoutingContext().request().params()
        .getAll(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);

    List<ITarget> targetList = findTargets(efContext, targets);
    long size = 0;
    for (ITarget target : targetList) {
      size += target.getSize();
    }
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_SIZE, size);
    handler.handle(createFuture(targetList));
  }

}
