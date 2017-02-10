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
package de.braintags.netrelay.controller.filemanager.elfinder.io.impl;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITargetSerializer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TargetSerializer implements ITargetSerializer<JsonObject> {
  private static final String CMD_TMB_TARGET = "?cmd=tmb&target=%s";

  @Override
  public JsonObject serialize(ElFinderContext efContext, ITarget<JsonObject> target) {
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

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.braintags.netrelay.controller.filemanager.elfinder.io.ITargetSerializer#serializeoptions(de.braintags.netrelay.
   * controller.filemanager.elfinder.ElFinderContext, de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget)
   */
  @Override
  public JsonObject serializeoptions(ElFinderContext efContext, ITarget<JsonObject> target) {
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
