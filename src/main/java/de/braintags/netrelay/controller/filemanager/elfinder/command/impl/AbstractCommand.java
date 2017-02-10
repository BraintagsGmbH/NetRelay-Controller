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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.ICommandListener;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * abstract implementation of ICommand
 * 
 * @author Michael Remme
 * @param <T>
 *          defines the return type of the abstract method
 *          {@link AbstractCommand#execute(ElFinderContext, JsonObject, Handler)}
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractCommand<T> implements ICommand {
  private ICommandListener listener;

  @Override
  public final void execute(ElFinderContext efContext, Handler<AsyncResult<JsonObject>> handler) {
    listenerBefore(efContext, lb -> {
      if (lb.failed()) {
        handler.handle(Future.failedFuture(lb.cause()));
      } else if (lb.result()) {
        doExecute(efContext, handler);
      } else {
        // command denied
        handler.handle(Future.failedFuture("action denied by server"));
      }
    });

  }

  private void doExecute(ElFinderContext efContext, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject json = new JsonObject();
    try {
      execute(efContext, json, res -> {
        if (res.failed()) {
          handler.handle(Future.failedFuture(res.cause()));
        } else {
          listenerAfter(efContext, json, res.result(), handler);
        }
      });
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  private void listenerBefore(ElFinderContext efContext, Handler<AsyncResult<Boolean>> handler) {
    if (listener != null) {
      listener.before(this, efContext, handler);
    } else {
      handler.handle(Future.succeededFuture(true));
    }
  }

  private void listenerAfter(ElFinderContext efContext, JsonObject json, T result,
      Handler<AsyncResult<JsonObject>> handler) {
    if (listener != null) {
      listener.after(this, efContext, result, json, lr -> {
        if (lr.failed()) {
          handler.handle(Future.failedFuture(lr.cause()));
        } else {
          handler.handle(Future.succeededFuture(json));
        }
      });
    } else {
      handler.handle(Future.succeededFuture(json));
    }
  }

  /**
   * Execute and fill the given JsonObject
   * 
   * @param efContext
   * @param json
   * @param handler
   *          the handler is getting the elements, which were used for the action, like an {@link ITarget} or a
   *          {@link List} of targets for instance
   */
  protected abstract void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<T>> handler);

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
   * Find all targets for the given hashes
   * 
   * @param efContext
   * @param targetHashes
   * @return
   * @throws IOException
   */
  protected List<ITarget> findTargets(ElFinderContext efContext, List<String> targetHashes) {
    if (targetHashes != null) {
      List<ITarget> targets = new ArrayList<>(targetHashes.size());
      for (String targetHash : targetHashes) {
        ITarget target = findTarget(efContext, targetHash);
        if (target != null)
          targets.add(target);
      }
      return targets;
    }
    return Collections.emptyList();
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

  protected JsonArray buildJsonFilesArray(ElFinderContext efContext, ITarget target) {
    JsonArray returnArray = new JsonArray();
    returnArray.add(getTargetInfo(efContext, target));
    return returnArray;
  }

  protected JsonArray buildJsonFilesArray(ElFinderContext efContext, Collection<ITarget> targetList) {
    JsonArray returnArray = new JsonArray();
    targetList.forEach(target -> returnArray.add(getTargetInfo(efContext, target)));
    return returnArray;
  }

  /**
   * Create a suceeded future with the given target
   * 
   * @param target
   * @return
   */
  protected Future<ITarget> createFuture(ITarget target) {
    return Future.succeededFuture(target);
  }

  /**
   * Create a suceeded future with the given targets
   * 
   * @param targets
   * @return
   */
  protected Future<List<ITarget>> createFuture(List<ITarget> targets) {
    return Future.succeededFuture(targets);
  }

  /**
   * Create a suceeded future with the given targets
   * 
   * @param targets
   * @return
   */
  protected Future<Map<String, ITarget>> createFuture(Map<String, ITarget> targets) {
    return Future.succeededFuture(targets);
  }

  /**
   * Checks wether target is a directory and not empty
   * 
   * @param target
   * @return
   */
  protected boolean checkEmptyDirectory(ITarget target) {
    return target.isFolder() && !target.hasChildren();
  }

  /**
   * @deprecated
   */
  @Deprecated
  protected JsonObject getTargetInfo(ElFinderContext efContext, ITarget target) {
    return (JsonObject) target.getSerializer().serialize(efContext, target);
  }

  /**
   * @deprecated
   */
  @Deprecated
  protected JsonObject getOptions(ElFinderContext efContext, ITarget target) {
    return (JsonObject) target.getSerializer().serializeoptions(efContext, target);
  }

  /**
   * Copy the file or folder to the destination
   * 
   * @param src
   * @param dst
   * @throws IOException
   */
  protected void createAndCopy(ITarget src, ITarget dst) {
    FileSystem fs = src.getVolume().getFileSystem();
    if (src.isFolder()) {
      fs.copyRecursiveBlocking(src.getAbsolutePath(), dst.getAbsolutePath(), true);
    } else {
      fs.copyBlocking(src.getAbsolutePath(), dst.getAbsolutePath());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand#addListener(de.braintags.netrelay.controller
   * .filemanager.elfinder.ICommandListener)
   */
  @Override
  public void addListener(ICommandListener commandListener) {
    this.listener = commandListener;
  }

}
