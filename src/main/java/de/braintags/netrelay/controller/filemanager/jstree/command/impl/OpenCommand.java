/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.filemanager.jstree.command.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class OpenCommand extends de.braintags.netrelay.controller.filemanager.elfinder.command.impl.OpenCommand {

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.braintags.netrelay.controller.filemanager.elfinder.command.impl.OpenCommand#fillJsonObject(de.braintags.netrelay
   * .controller.filemanager.elfinder.ElFinderContext, io.vertx.core.json.JsonObject)
   */
  @Override
  protected Map<String, ITarget> fillJsonObject(ElFinderContext efContext, JsonObject json) {
    RoutingContext context = efContext.getRoutingContext();
    boolean init = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_INIT) != null;
    boolean tree = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_TREE) != null;
    String target = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_TARGET);

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
    JsonObject cwdOb = (JsonObject) cwd.getSerializer().serialize(efContext, cwd);
    json.mergeIn(cwdOb);
    return files;
  }

}
