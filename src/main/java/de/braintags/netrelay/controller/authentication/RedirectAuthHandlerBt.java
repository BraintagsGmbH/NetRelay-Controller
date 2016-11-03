/*
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.authentication;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.braintags.io.vertx.util.security.CRUDPermissionMap;
import de.braintags.netrelay.RequestUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.RedirectAuthHandlerImpl;

/**
 * Implementation of AuthHandler based on vertx {@link RedirectAuthHandlerImpl}, which adds the url parameters into the
 * page, which is aimed after a successful login.
 * Additionally the method authorise is overwritten, cause the default implementation expects ALL permissions to be
 * true, this implementation expects ONE permission to be true
 * 
 * @author Michael Remme
 * 
 */
public class RedirectAuthHandlerBt extends AuthHandlerImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectAuthHandlerImpl.class);

  private final String loginRedirectURL;
  private final String returnURLParam;
  private CRUDPermissionMap permissionMap;

  public RedirectAuthHandlerBt(AuthProvider authProvider, String loginRedirectURL, String returnURLParam) {
    super(authProvider);
    this.loginRedirectURL = loginRedirectURL;
    this.returnURLParam = returnURLParam;
  }

  @Override
  public void handle(RoutingContext context) {
    Session session = context.session();
    if (session != null) {
      User user = context.user();
      if (user != null) {
        // Already logged in, just authorise
        authorise(user, context);
      } else {
        // Now redirect to the login url - we'll get redirected back here after successful login
        String url = RequestUtil.createRedirectUrl(context, context.request().path());
        session.put(returnURLParam, url);
        context.response().putHeader("location", loginRedirectURL).setStatusCode(302).end();
      }
    } else {
      context.fail(new NullPointerException("No session - did you forget to include a SessionHandler?"));
    }

  }

  @Override
  protected void authorise(User user, RoutingContext context) {
    int requiredcount = authorities.size();
    if (requiredcount > 0) {
      AtomicInteger count = new AtomicInteger();
      AtomicBoolean stopLoop = new AtomicBoolean();

      Handler<AsyncResult<Boolean>> authHandler = res -> {
        if (res.failed()) {
          stopLoop.set(true);
          context.fail(res.cause());
        } else {
          if (res.result()) {
            // Has ONE required authorities
            stopLoop.set(true);
            LOGGER.info("one authority fits: access granted");
            context.next();
          } else if (count.incrementAndGet() == requiredcount) {
            // Has none of the required authorities
            LOGGER.info("none of the authorities was fitting - access forbidden");
            context.fail(403);
          }
        }
      };

      for (String authority : authorities) {
        if ("role:*".equals(authority)) {
          // Wildcard role - grant access
          stopLoop.set(true);
          LOGGER.info("Wildcard role authority found: access granted");
          context.next();
        } else {
          user.isAuthorised(authority, authHandler);
        }
        if (stopLoop.get()) {
          break;
        }
      }
    } else {
      // No auth required
      context.next();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.web.handler.impl.AuthHandlerImpl#addAuthority(java.lang.String)
   */
  @Override
  public AuthHandler addAuthority(String authority) {
    if (authority.startsWith("role:")) {
      // this creates the pure role authority without permissions: role:admin{CRUD} -> role:admin
      String rolePermission = "role:" + addRoleAuthority(authority.substring(5));
      return super.addAuthority(rolePermission);
    } else {
      return super.addAuthority(authority);
    }
  }

  private String addRoleAuthority(String permission) {
    CRUDPermissionMap cm = getPermissionMap();
    return cm.addPermissionEntry(permission);
  }

  /**
   * Get the permission map
   * 
   * @return the permissionMap
   */
  public CRUDPermissionMap getPermissionMap() {
    if (permissionMap == null) {
      permissionMap = new CRUDPermissionMap();
    }
    return permissionMap;
  }

}
