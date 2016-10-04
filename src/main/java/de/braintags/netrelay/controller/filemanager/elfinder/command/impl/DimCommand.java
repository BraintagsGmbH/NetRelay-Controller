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

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * Returns dimensions of an image
 * 
 * @author Michael Remme
 * 
 */
public class DimCommand extends AbstractCommand {
  public static final String SEPARATOR = "x";

  @Override
  public void execute(ElFinderContext efContext, JsonObject json, Handler<AsyncResult<Void>> handler) {
    final String targetString = efContext.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);

    BufferedImage image;
    ITarget target = findTarget(efContext, targetString);
    try {
      image = ImageIO.read(target.openInputStream());
      json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_DIM, image.getWidth() + SEPARATOR + image.getHeight());
    } catch (IOException e) {
      handler.handle(Future.failedFuture(e));
    }
  }
}
