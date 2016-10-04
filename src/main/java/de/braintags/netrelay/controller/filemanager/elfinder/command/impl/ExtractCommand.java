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

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class ExtractCommand extends AbstractCommand {
  public static final String STREAM = "1";

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.failedFuture(new UnsupportedOperationException()));
  }

}
