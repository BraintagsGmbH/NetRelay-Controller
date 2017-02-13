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
package de.braintags.netrelay.controller.filemanager.jstree.command;

import de.braintags.netrelay.controller.filemanager.elfinder.command.CommandFactory;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class JsCommandFactory extends CommandFactory {

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.command.CommandFactory#getCommand(java.lang.String)
   */
  @Override
  public ICommand getCommand(String commandName) {
    return super.getCommand(commandName);
  }

}
