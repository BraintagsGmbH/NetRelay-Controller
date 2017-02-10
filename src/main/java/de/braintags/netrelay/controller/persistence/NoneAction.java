/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.persistence;

import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This action is just doing nothing and can be used to call a page, where a form inside, which shall not be processed,
 * for instance
 * 
 * @author Michael Remme
 * 
 */
public class NoneAction extends AbstractAction {

  /**
   * @param persitenceController
   */
  public NoneAction(PersistenceController persitenceController) {
    super(persitenceController);
  }

  @Override
  protected void handleRegularEntityDefinition(String entityName, RoutingContext context, CaptureMap captureMap,
      IMapper<?> mapper, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  @Override
  protected void handleSubobjectEntityDefinition(RoutingContext context, String entityName, CaptureMap captureMap,
      IMapper<?> mapper, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

}
