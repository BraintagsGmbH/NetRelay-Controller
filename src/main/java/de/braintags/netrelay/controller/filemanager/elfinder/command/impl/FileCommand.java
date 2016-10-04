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
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class FileCommand extends AbstractCommand {
  public static final String STREAM = "1";

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    String target = efContext.getParameter("target");
    boolean download = STREAM.equals(efContext.getParameter("download"));
    ITarget fsi = super.findTarget(efContext, target);
    HttpServerResponse response = efContext.getRoutingContext().response();
    if (download) {
      response.sendFile(fsi.getPath(), handler);
    } else {
      String mime = fsi.getMimeType();
      response.putHeader("content-type", mime + "; charset=utf-8");
      response.end(fsi.readFile());
    }
  }

}
