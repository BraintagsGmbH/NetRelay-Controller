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

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * 
 * A command of ElFinder, which must be executed to deliver contents to the frontend
 * 
 * @author Michael Remme
 * 
 */
public interface ICommand {

  /**
   * Executes the command and returns the resulting Json object as result to the handler
   * 
   * @param context
   * @param efContext
   * @param handler
   */
  public void execute(ElFinderContext efContext, Handler<AsyncResult<JsonObject>> handler);
}
