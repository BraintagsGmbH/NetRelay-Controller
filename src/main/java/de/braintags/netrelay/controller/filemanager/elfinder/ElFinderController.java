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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.filemanager.elfinder.command.CommandFactory;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.TargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import de.braintags.vertx.util.exception.InitException;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

/**
 * This controller builds the api to support the web base filemanager from
 * https://github.com/Studio-42/elFinder
 * 
 * 
 * <br/>
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <UL>
 * <LI>rootDirectories - defines directories to be used in the form of VolumeId:rootDirectory.
 * Note: the path of the defined root directory in the example above defines the name of the volume before the colon,
 * like it is displayed in the elfinder component. comma separated list
 * <LI>{@value #IGNORES} used to define subfiles and directories, which shall be omitted. comma
 * separated list of regex patterns
 * </UL>
 * <br>
 * 
 * Request-Parameter:<br/>
 * the request parameters are defined by the required api like described at
 * https://github.com/Studio-42/elFinder/wiki/Client-Server-API-2.1
 * <br/>
 * 
 * Result-Parameter:<br/>
 * results are sent like required by the ElFinder component for the suitable command
 * 
 * Example configuration:<br/>
 * 
 * <pre>
 * 
   {
      "name" : "ElFinderController",
      "controller" : "de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController",
      "routes" : [ "/fileManager/api"  ],
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
public class ElFinderController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ElFinderController.class);
  protected CommandFactory commandFactory = new CommandFactory();

  /**
   * Defines the root directories as comma separated list, which shall be used for serving contents
   */
  public static final String ROOT_DIRECTORIES_PROPERTY = "rootDirectories";

  /**
   * used to define subfiles and directories of the root directory, which shall be omitted
   */
  public static final String IGNORES = "ignores";

  private List<IVolume> rootVolumes = new ArrayList<>();
  protected List<String> ignores;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    LOGGER.debug("PARAMETERS: " + context.request().params());
    LOGGER.debug("HEADERS: " + context.request().headers());
    String commandName = context.request().getParam(ElFinderConstants.ELFINDER_PARAMETER_COMMAND);
    ICommand command = commandFactory.getCommand(commandName);
    if (command == null) {
      sendJson(context, ElFinderConstants.ELFINDER_ERROR_UNKNOWN_CONTROLLER);
    } else {
      try {
        ElFinderContext efContext = createContext(context);
        command.execute(efContext, res -> {
          if (res.failed()) {
            sendException(context, res.cause());
          } else {
            if (!context.response().ended()) { // some commands, like File, may send already another result
              sendJson(context, res.result().encode());
            }
          }
        });
      } catch (Exception e) {
        sendException(context, e);
      }
    }
  }

  /**
   * Add an {@link ICommandListener}
   * 
   * @param command
   * @param listener
   */
  public void addCommandListener(String command, ICommandListener listener) {
    commandFactory.addCommandListener(command, listener);
  }

  protected void sendException(RoutingContext context, Throwable e) {
    LOGGER.error("", e);
    String message = String.format(ElFinderConstants.ELFINDER_ERROR_EXCEPTION, e.toString());
    sendJson(context, message);
  }

  protected ElFinderContext createContext(RoutingContext context) {
    return new ElFinderContext(context, rootVolumes, ignores);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    String ign = readProperty(IGNORES, null, false);
    if (ign != null) {
      String[] arr = ign.split(",");
      ignores = Arrays.asList(arr);
    }
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
      Path path = FileSystems.getDefault().getPath(dirName);
      LOGGER.debug("ElFinder-Path: " + path);
      if (dirName.endsWith("/")) {
        dirName = dirName.substring(0, dirName.length() - 1);
      }
      rootVolumes.add(createVolume(fs, volumeId, path));
    }

  }

  /**
   * @param fs
   * @param volumeId
   * @param path
   * @return
   */
  protected VertxVolume createVolume(FileSystem fs, String volumeId, Path path) {
    return new VertxVolume(fs, path, volumeId, null, new TargetSerializer(), ignores);
  }

}
