package de.braintags.netrelay.controller.authentication.loginhandler;

import java.util.Properties;

import io.vertx.core.Handler;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.RoutingContext;

/**
 * Interface for handlers that execute a login with the given {@link AuthProvider}, and optionally handle the user on
 * successful login, i.e. session, context, redirects, ...
 * 
 * @author sschmitt
 * 
 */
public interface LoginHandler extends Handler<RoutingContext> {

  /**
   * Initialize the handler
   * 
   * @param authProvider
   *          the provider that authenticates the user
   * @param properties
   *          properties to initialize the handler
   */
  public void init(AuthProvider authProvider, Properties properties);

}
