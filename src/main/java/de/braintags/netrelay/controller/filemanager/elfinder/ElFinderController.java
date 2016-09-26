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
package de.braintags.netrelay.controller.filemanager.elfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.braintags.io.vertx.util.exception.InitException;
import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.IController;
import de.braintags.netrelay.controller.filemanager.elfinder.command.CommandFactory;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

/**
 * An abstract implementation of {@link IController}
 * 
 * <br/>
 * Documentation Template for Controllers:<br/>
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <UL>
 * <LI>rootDirectories - define directories to be used in the form of VolumeId:rootDirectory
 * <LI>parameter2 - describe the sense of the parameter
 * </UL>
 * <br>
 * 
 * Request-Parameter:<br/>
 * possible parameters, which are read from a request
 * <UL>
 * <LI>parameter1 - describe the sense of the parameter
 * <LI>parameter2 - describe the sense of the parameter
 * </UL>
 * <br/>
 * 
 * Result-Parameter:<br/>
 * possible paramters, which will be placed into the context
 * <UL>
 * <LI>parameter1 - describe the content, which is stored under the given parameter name
 * </UL>
 * <br/>
 * 
 * Example configuration:<br/>
 * 
 * <pre>
 * {
      "name" : "ExampleController",
      "routes" : null,
      "blocking" : false,
      "failureDefinition" : false,
      "controller" : "de.braintags.netrelay.controller.ExampleController",
      "httpMethod" : null,
      "handlerProperties" : {
        "prop1" : "127.0.0.1",
        "prop2" : "http://localhost",
        "prop3" : "true"
       },
      "captureCollection" : null
    }
 * </pre>
 * 
 * 
 * 
 * @author Michael Remme
 * 
 */
public class ElFinderController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ElFinderController.class);
  private CommandFactory commandFactory = new CommandFactory();
  /**
   * Defines the root directories as comma separated list, which shall be used for serving contents
   */
  public static final String ROOT_DIRECTORIES_PROPERTY = "rootDirectories";

  private List<IVolume> rootVolumes = new ArrayList<>();

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("PARAMETERS: " + context.request().params());
    LOGGER.debug("HEADERS: " + context.request().headers());
    String commandName = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_COMMAND);
    ICommand command = commandFactory.getCommand(commandName);
    if (command == null) {
      sendJson(context, ElFinderConstants.ELFINDER_ERROR_UNKNOWN_CONTROLLER);
    } else {
      ElFinderContext efContext = createContext(context);
      command.execute(efContext, res -> {
        if (res.failed()) {
          LOGGER.error("", res.cause());
          String message = String.format(ElFinderConstants.ELFINDER_ERROR_EXCEPTION, res.cause().toString());
          sendJson(context, message);
        } else {
          sendJson(context, res.result().encode());
        }
      });
    }
  }

  private ElFinderContext createContext(RoutingContext context) {
    return new ElFinderContext(context, rootVolumes);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    String dirs = readProperty(ROOT_DIRECTORIES_PROPERTY, null, true);
    String[] volSpecs = dirs.split(",");
    for (String volSpec : volSpecs) {
      String[] specs = volSpec.trim().split(":");
      FileSystem fs = getVertx().fileSystem();
      String volumeId = specs[0].trim();
      String dirName = specs[1].trim();
      if (!fs.existsBlocking(dirName)) {
        throw new InitException("Root directory with path '" + dirName + "' does not exist");
      }
      rootVolumes.add(new VertxVolume(fs, dirName, volumeId, null));
    }
  }

}
