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
package de.braintags.netrelay.controller.filemanager.elfinder.command;

import java.util.HashMap;
import java.util.Map;

import de.braintags.netrelay.controller.filemanager.elfinder.ICommandListener;

/**
 * A factory to generate instances of {@link ICommand} for the api of ElFinder
 * 
 * @author Michael Remme
 * 
 */
public class CommandFactory {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(CommandFactory.class);
  private static final String CLASSNAME_PATTERN = "%s.impl.%sCommand";
  private Map<String, ICommand> commandMap = new HashMap<>();
  private Map<String, ICommandListener> commandListenerMap = new HashMap<>();

  public ICommand getCommand(String commandName) {
    if (commandName == null || commandName.trim().isEmpty()) {
      LOGGER.error(String.format("Command %s cannot be null or empty", commandName));
      throw new RuntimeException(String.format("Command %s cannot be null or empty", commandName));
    }
    ICommand command = null;
    try {
      command = commandMap.get(commandName);
      if (command == null) {
        command = generateCommand(commandName);
        if (commandListenerMap.containsKey(commandName)) {
          command.addListener(commandListenerMap.get(commandName));
        }
        commandMap.put(commandName, command);
      }
      LOGGER.debug(String.format("command found!: %s", commandName));
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      LOGGER.error("Unable to get/create command instance.", e);
    }
    return command;
  }

  /**
   * Add a listener for the given command
   * 
   * @param command
   *          a command from the api, like "file", "get", "rm" etc.
   * @param listener
   *          a listener which will be called, when the fitting command is executed
   */
  public void addCommandListener(String command, ICommandListener listener) {
    commandListenerMap.put(command, listener);
    if (commandMap.containsKey(command)) {
      commandMap.get(command).addListener(listener);
    }
  }

  @SuppressWarnings("unchecked")
  protected ICommand generateCommand(String commandName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String packageName = getClass().getPackage().getName();
    Class<? extends ICommand> commandClass = null;
    try {
      String commandClassName = generateCommandClassName(packageName, commandName);
      commandClass = (Class<? extends ICommand>) Class.forName(commandClassName);
    } catch (ClassNotFoundException e) {
      // extending class of CommandFactory: command not found
      LOGGER.warn(e);
    }
    if (commandClass == null) {
      commandClass = (Class<? extends ICommand>) Class.forName(
          generateCommandClassName("de.braintags.netrelay.controller.filemanager.elfinder.command", commandName));
    }
    return commandClass.newInstance();
  }

  protected String generateCommandClassName(String packageName, String commandName) {
    String simpleName = commandName.substring(0, 1).toUpperCase() + commandName.substring(1);
    return String.format(CLASSNAME_PATTERN, packageName, simpleName);
  }

}
