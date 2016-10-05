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

import java.util.ArrayList;
import java.util.List;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class PasteCommand extends AbstractCommand {
  public static final String INT_CUT = "1";

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    List<String> targets = efContext.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);
    final String destination = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_FILE_DESTINATION);
    final boolean cut = INT_CUT.equals(efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_CUT));

    List<ITarget> added = new ArrayList<>();
    List<String> removed = new ArrayList<>();

    ITarget vhDst = findTarget(efContext, destination);

    for (String target : targets) {
      ITarget vhTarget = findTarget(efContext, target);
      final String name = vhTarget.getName();
      ITarget newFile = vhDst.createChildTarget(name);
      createAndCopy(vhTarget, newFile);
      added.add(newFile);

      if (cut) {
        vhTarget.delete();
        removed.add(vhTarget.getHash());
      }
    }

    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, buildJsonFilesArray(efContext, added));
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_REMOVED, removed.toArray());
    handler.handle(Future.succeededFuture());
  }

}
