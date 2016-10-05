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

import java.util.LinkedHashMap;
import java.util.Map;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class OpenCommand extends AbstractCommand {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    RoutingContext context = efContext.getRoutingContext();
    boolean init = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_INIT) != null;
    boolean tree = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_TREE) != null;
    String target = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
    if (init) {
      json.put(ElFinderConstants.ELFINDER_PARAMETER_API, ElFinderConstants.ELFINDER_VERSION_API);
      json.put(ElFinderConstants.ELFINDER_PARAMETER_NETDRIVERS, new JsonArray());
    }

    Map<String, ITarget> files = new LinkedHashMap<>();

    if (tree) {
      for (IVolume root : efContext.getRootVolumes()) {
        ITarget rootTarget = root.getRoot();
        String hash = ElFinderContext.getHash(rootTarget);
        files.put(hash, rootTarget);
        addSubFolders(efContext, files, rootTarget);
      }
    }

    ITarget cwd = findCwd(efContext, target);
    files.put(cwd.getHash(), cwd);
    addChildren(efContext, files, cwd);
    json.put(ElFinderConstants.ELFINDER_PARAMETER_FILES, buildJsonFilesArray(efContext, files.values()));
    json.put(ElFinderConstants.ELFINDER_PARAMETER_CWD, getTargetInfo(efContext, cwd));
    json.put(ElFinderConstants.ELFINDER_PARAMETER_OPTIONS, getOptions(efContext, cwd));
    handler.handle(Future.succeededFuture());
  }

}
