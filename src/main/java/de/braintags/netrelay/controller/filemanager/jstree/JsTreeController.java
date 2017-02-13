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
 * This controller builds the api to support the web base tree manager https://www.jstree.com/
 * It extends {@link ElFinderController} and thus supports the complete api of ElFinder and sends responses like needed
 * by JSTree
 * 
 * 
 * <br/>
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <UL>
 * <LI>rootDirectories - defines directories to be used in the form of VolumeId:rootDirectory.
 * Note: the path of the defined root directory in the example above defines the name of the volume before the colon,
 * like it is displayed in the elfinder component.
 * 
 * </UL>
 * <br>
 * 
 * Request-Parameter:<br/>
 * the request parameters are defined by the required api like described at
 * https://github.com/Studio-42/elFinder/wiki/Client-Server-API-2.1
 * <br/>
 * 
 * Result-Parameter:<br/>
 * results are sent like required by the JSTree component for the suitable command
 * 
 * Example configuration:<br/>
 * 
 * <pre>
 * 
   {
      "name" : "JsTreeController",
      "controller" : "de.braintags.netrelay.controller.filemanager.jstree.JsTreeController",
      "routes" : [ "/fileManager/api/jstree"  ],
      "handlerProperties" : { 
        "rootDirectories" : "ROOTVOLUME:webroot"
      }
    }
 * </pre>
 * 
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
