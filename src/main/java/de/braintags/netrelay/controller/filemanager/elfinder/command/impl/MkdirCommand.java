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

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class MkdirCommand extends AbstractCommand {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    final String targetHash = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
    final String fileName = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_NAME);
    ITarget target = findTarget(efContext, targetHash);
    ITarget newFile = target.createChildTarget(fileName);
    newFile.createFile();
    JsonObject jo = getTargetInfo(efContext, newFile);
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, new JsonArray().add(jo));
    handler.handle(Future.succeededFuture());
  }

}
