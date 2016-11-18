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

import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * ICommandListener can be used to register listener to a certain command, which is executed by the api of El-Finder
 * 
 * @author Michael Remme
 * 
 */
public interface ICommandListener {

  /**
   * This method is called, when the command was executed
   * 
   * @param command
   *          the command, which was executed
   * @param context
   *          the context for the request
   * @param target
   *          the target or the targets - depending on the command - which were used for processing
   * @param resultObject
   *          the resultObject which was produced by the command
   * @param handler
   *          a handler to be informed
   */
  public void executed(ICommand command, ElFinderContext context, Object target, JsonObject resultObject,
      Handler<AsyncResult<Void>> handler);

}
