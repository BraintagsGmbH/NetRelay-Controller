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
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
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
public class SearchCommand extends AbstractCommand {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    final String query = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_SEARCH_QUERY);
    try {
      JsonArray objects = new JsonArray();
      List<IVolume> volumes = efContext.getRootVolumes();
      for (IVolume volume : volumes) {
        // checks volume security
        ITarget volumeRoot = volume.getRoot();
        // search only in volumes that are readable
        if (volumeRoot.isReadable()) {
          // search for targets
          List<ITarget> targets = volume.search(query);
          if (targets != null) {
            // adds targets info in the return list
            targets.forEach(target -> objects.add(getTargetInfo(efContext, target)));
          }
        }
      }

      json.put(ElFinderConstants.ELFINDER_PARAMETER_FILES, objects);
    } catch (Exception e) {
      json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, "Unable to search! Error: " + e);
    }
    handler.handle(Future.succeededFuture());
  }

}
