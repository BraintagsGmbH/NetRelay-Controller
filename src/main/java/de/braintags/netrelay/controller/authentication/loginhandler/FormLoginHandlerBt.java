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
package de.braintags.netrelay.controller.authentication.loginhandler;

import java.util.Properties;

import de.braintags.netrelay.controller.authentication.AbstractAuthProviderController;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FormLoginHandler;

/**
 * An extension of FormLoginHandlerImpl cause to the handling of failed logins
 * 
 * @author Michael Remme
 * 
 */
public class FormLoginHandlerBt extends AbstractLoginHandler implements FormLoginHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(FormLoginHandlerBt.class);

  private String usernameField;
  private String passwordField;

  @Override
  public void init(AuthProvider authProvider, Properties properties) {
    super.init(authProvider, properties);
    this.usernameField = properties.getProperty(AbstractAuthProviderController.USERNAME_FIELD, DEFAULT_USERNAME_PARAM);
    this.passwordField = properties.getProperty(AbstractAuthProviderController.PASSWORD_FIELD, DEFAULT_PASSWORD_PARAM);
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest req = context.request();
    if (req.method() != HttpMethod.POST) {
      context.fail(405); // Must be a POST
    } else {
      if (!req.isExpectMultipart()) {
        throw new IllegalStateException("Form body not parsed - did you forget to include a BodyHandler?");
      }
      MultiMap params = req.formAttributes();
      String username = params.get(usernameField);
      String password = params.get(passwordField);
      if (username == null || password == null) {
        LOGGER.warn("No username or password provided in form - did you forget to include a BodyHandler?");
        context.fail(400);
      } else {
        JsonObject authInfo = new JsonObject().put(usernameField, username).put(passwordField, password);
        authenticate(authInfo, null, context);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.web.handler.FormLoginHandler#setUsernameParam(java.lang.String)
   */
  @Override
  public FormLoginHandler setUsernameParam(String usernameParam) {
    this.usernameField = usernameParam;
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.web.handler.FormLoginHandler#setPasswordParam(java.lang.String)
   */
  @Override
  public FormLoginHandler setPasswordParam(String passwordParam) {
    this.passwordField = passwordParam;
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.web.handler.FormLoginHandler#setDirectLoggedInOKURL(java.lang.String)
   */
  @Override
  public FormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL) {
    this.directLoggedInOKURL = directLoggedInOKURL;
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.web.handler.FormLoginHandler#setReturnURLParam(java.lang.String)
   */
  @Override
  public FormLoginHandler setReturnURLParam(String returnURLParam) {
    this.returnURLParam = returnURLParam;
    return this;
  }
}
