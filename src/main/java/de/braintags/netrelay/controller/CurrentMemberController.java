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
package de.braintags.netrelay.controller;

import java.util.Properties;

import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.model.IAuthenticatable;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * If a user is logged in, the propriate record is fetched from the datastore and stored as
 * {@link Member#CURRENT_USER_PROPERTY} in the context. Extensions of this class may overwrite the method
 * {@link #loadMemberData(Member, RoutingContext, Handler)} to load additional data.
 * 
 * <br>
 * <br>
 * Config-Parameter:<br/>
 * <br>
 * Request-Parameter:<br/>
 * <br/>
 * Result-Parameter:<br/>
 * {@link Member#CURRENT_USER_PROPERTY} in the context<br/>
 * 
 * @author Michael Remme
 */
public class CurrentMemberController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(CurrentMemberController.class);

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public final void handle(RoutingContext context) {
    LOGGER.debug("Session-ID: " + context.session().id() + " for request " + context.request().path());
    MemberUtil.getCurrentUser(context, getNetRelay(), res -> {
      if (res.failed()) {
        context.fail(res.cause());
      } else {
        IAuthenticatable member = res.result();
        context.put(IAuthenticatable.CURRENT_USER_PROPERTY, member);
        loadMemberData(member, context, dataResult -> {
          if (dataResult.failed()) {
            context.fail(dataResult.cause());
          } else {
            context.next();
          }
        });
      }
    });
  }

  /**
   * Extensions may load additional data for the current member
   * 
   * @param member
   *          the member, if logged in or null
   * @param context
   *          the context of the current request
   * @param handler
   *          the handler to be informed
   */
  protected void loadMemberData(IAuthenticatable member, RoutingContext context, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(CurrentMemberController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(CurrentMemberController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(null);
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    return new Properties();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    // nothing to do here
  }
}
