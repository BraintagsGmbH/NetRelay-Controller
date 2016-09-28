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
public class RmCommand extends AbstractCommand {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    List<String> targets = efContext.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);
    JsonArray removed = new JsonArray();
    for (String ts : targets) {
      ITarget target = findTarget(efContext, ts);
      if (!target.isFolder() || checkEmptyDirectory(target)) {
        target.delete();
        removed.add(target.getHash());
      } else {
        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, "Directory not empty: " + target.getPath());
      }
    }
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_REMOVED, removed);
    handler.handle(Future.succeededFuture());
  }

  /**
   * Checks wether target is a directory and not empty
   * 
   * @param target
   * @return
   */
  private boolean checkEmptyDirectory(ITarget target) {
    return target.isFolder() && !target.hasChildren();
  }

}
