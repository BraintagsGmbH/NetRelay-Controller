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

import org.apache.commons.io.FilenameUtils;

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
public class DuplicateCommand extends AbstractCommand {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    List<String> targets = efContext.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);

    List<ITarget> added = new ArrayList<>();

    for (String targetString : targets) {
      final ITarget source = findTarget(efContext, targetString);
      final String name = source.getName();
      String baseName = FilenameUtils.getBaseName(name);
      final String extension = FilenameUtils.getExtension(name);

      int i = 1;
      ITarget destination;
      baseName = baseName.replaceAll("\\(\\d+\\)$", "");

      while (true) {
        String newName = String.format("%s(%d)%s", baseName, i,
            extension == null || extension.isEmpty() ? "" : "." + extension);
        destination = source.getParent().createChildTarget(newName);

        if (!destination.exists()) {
          break;
        }
        i++;
      }

      createAndCopy(source, destination);
      added.add(destination);
    }
    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, buildJsonFilesArray(efContext, added));
    handler.handle(Future.succeededFuture());
  }

}
