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
package de.braintags.netrelay.controller.filemanager.jstree;

import java.nio.file.Path;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import io.vertx.core.file.FileSystem;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class JsTreeController extends ElFinderController {

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController#createVolume(io.vertx.core.file.
   * FileSystem, java.lang.String, java.nio.file.Path)
   */
  @Override
  protected VertxVolume createVolume(FileSystem fs, String volumeId, Path path) {
    return new VertxVolume(fs, path, volumeId, null, new JsTreeTargetSerializer());
  }

}
