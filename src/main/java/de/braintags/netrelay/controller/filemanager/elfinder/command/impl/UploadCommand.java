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
import java.util.Set;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.vertx.util.file.FileSystemUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class UploadCommand extends AbstractCommand<List<ITarget>> {

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<List<ITarget>>> handler) {
    String targetString = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
    ITarget parentDir = findTarget(efContext, targetString);
    Set<FileUpload> uploadedFiles = efContext.getRoutingContext().fileUploads();
    List<ITarget> added = new ArrayList<>();

    FileSystem fs = efContext.getRoutingContext().vertx().fileSystem();
    for (FileUpload upload : uploadedFiles) {
      String newFileName = FileSystemUtil.createUniqueName(fs, parentDir.getAbsolutePath(), upload.fileName());
      ITarget newFile = parentDir.createChildTarget(newFileName);
      fs.moveBlocking(upload.uploadedFileName(), newFile.getAbsolutePath());
      added.add(newFile);
    }

    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, buildJsonFilesArray(efContext, added));
    handler.handle(createFuture(added));
  }

}
