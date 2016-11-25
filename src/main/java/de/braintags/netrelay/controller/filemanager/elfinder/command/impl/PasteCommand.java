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
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Arguments are defined as:
 * 
 * cmd : paste
 * src : hash of the directory from which the files will be copied / moved (the source)
 * dst : hash of the directory to which the files will be copied / moved (the destination)
 * targets[] : An array of hashes for the files to be copied / moved
 * cut : 1 if the files are moved, missing if the files are copied
 * renames[] : Filename list of rename request
 * suffix : Suffixes during rename (default is "~")
 * 
 * @author Michael Remme
 * 
 */
public class PasteCommand extends AbstractCommand<List<ITarget>> {
  public static final String INT_CUT = "1";

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<List<ITarget>>> handler) {
    List<String> targets = efContext.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);
    List<String> renames = efContext.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_RENAMES);
    final String destination = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_FILE_DESTINATION);
    final String src = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_SRC);
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

    JsonArray returnArray = new JsonArray();
    removed.forEach(target -> returnArray.add(target));
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_REMOVED, returnArray);
    handler.handle(createFuture(added));
  }

}
