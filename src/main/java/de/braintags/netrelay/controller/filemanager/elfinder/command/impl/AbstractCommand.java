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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * abstract implementation of ICommand
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractCommand implements ICommand {
  private static final String CMD_TMB_TARGET = "?cmd=tmb&target=%s";

  @Override
  public final void execute(ElFinderContext efContext, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject();
    execute(efContext, json, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(json));
      }
    });
  }

  /**
   * Execute and fill the given JsonObject
   * 
   * @param context
   * @param efContext
   * @param json
   * @param handler
   */
  protected abstract void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler);

  /**
   * Find the current working directory - the directory for the current request
   * 
   * @param efContext
   * @param target
   * @return
   * @throws IOException
   */
  protected ITarget findCwd(ElFinderContext efContext, String targetHash) {
    ITarget cwd = null;
    if (targetHash != null) {
      cwd = findTarget(efContext, targetHash);
    }

    if (cwd == null) {
      return efContext.getRootVolumes().get(0).getRoot();
    }

    return cwd;
  }

  /**
   * Find the target with the given hash
   * 
   * @param efContext
   * @param targetHash
   * @return
   * @throws IOException
   */
  protected ITarget findTarget(ElFinderContext efContext, String targetHash) {
    return efContext.fromHash(targetHash);
  }

  /**
   * Recursively add folders of the given parent to the map
   * 
   * @param efContext
   * @param map
   * @param parent
   * @param handler
   */
  protected void addSubFolders(ElFinderContext efContext, Map<String, ITarget> map, ITarget parent) {
    List<ITarget> children = parent.listChildren();
    for (ITarget child : children) {
      if (child.isFolder()) {
        map.put(child.getHash(), child);
        addSubFolders(efContext, map, child);
      }
    }
  }

  /**
   * Add the children of the given target to the map
   * 
   * @param efContext
   * @param map
   * @param target
   * @param handler
   */
  protected void addChildren(ElFinderContext efContext, Map<String, ITarget> map, ITarget target) {
    List<ITarget> targetList = target.listChildren();
    targetList.forEach(f -> map.put(f.getHash(), f));
  }

  protected JsonArray buildJsonFilesArray(ElFinderContext efContext, Collection<ITarget> targetList) {
    JsonArray returnArray = new JsonArray();
    targetList.forEach(target -> returnArray.add(getTargetInfo(efContext, target)));
    return returnArray;
  }

  protected JsonObject getTargetInfo(ElFinderContext efContext, ITarget target) {
    JsonObject info = new JsonObject();

    info.put(ElFinderConstants.ELFINDER_PARAMETER_HASH, target.getHash());
    info.put(ElFinderConstants.ELFINDER_PARAMETER_MIME, target.getMimeType());
    info.put(ElFinderConstants.ELFINDER_PARAMETER_TIMESTAMP, target.getLastModified());
    info.put(ElFinderConstants.ELFINDER_PARAMETER_SIZE, target.getSize());
    info.put(ElFinderConstants.ELFINDER_PARAMETER_READ,
        target.isReadable() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
    info.put(ElFinderConstants.ELFINDER_PARAMETER_WRITE,
        target.isWritable() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
    info.put(ElFinderConstants.ELFINDER_PARAMETER_LOCKED,
        target.isLocked() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);

    if (target.getMimeType() != null && target.getMimeType().startsWith("image")) {
      String uri = efContext.getRoutingContext().request().absoluteURI()
          + String.format(CMD_TMB_TARGET, target.getHash());
      info.put(ElFinderConstants.ELFINDER_PARAMETER_THUMBNAIL, uri);
    }

    if (target.isRoot()) {
      info.put(ElFinderConstants.ELFINDER_PARAMETER_DIRECTORY_FILE_NAME, target.getVolume().getAlias());
      info.put(ElFinderConstants.ELFINDER_PARAMETER_VOLUME_ID, target.getVolume().getId());
    } else {
      info.put(ElFinderConstants.ELFINDER_PARAMETER_DIRECTORY_FILE_NAME, target.getName());
      info.put(ElFinderConstants.ELFINDER_PARAMETER_PARENTHASH, target.getParent().getHash());
    }

    if (target.isFolder()) {
      info.put(ElFinderConstants.ELFINDER_PARAMETER_HAS_DIR, target.hasChildFolder()
          ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
    }
    return info;
  }

  protected JsonObject getOptions(ElFinderContext efContext, ITarget target) {
    JsonObject options = new JsonObject();
    options.put(ElFinderConstants.ELFINDER_PARAMETER_PATH, target.getName());
    options.put(ElFinderConstants.ELFINDER_PARAMETER_COMMAND_DISABLED, new JsonArray());
    options.put(ElFinderConstants.ELFINDER_PARAMETER_FILE_SEPARATOR,
        ElFinderConstants.ELFINDER_PARAMETER_FILE_SEPARATOR);
    options.put(ElFinderConstants.ELFINDER_PARAMETER_OVERWRITE_FILE, ElFinderConstants.ELFINDER_TRUE_RESPONSE);
    // options.put(ElFinderConstants.ELFINDER_PARAMETER_ARCHIVERS, ArchiverOption.JSON_INSTANCE);
    return options;
  }

}
