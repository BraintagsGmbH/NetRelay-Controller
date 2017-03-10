package de.braintags.netrelay.controller.authentication;

import java.util.Properties;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;

/**
 * Interface for auth providers used by the {@link AuthenticationController}
 * 
 * @author sschmitt
 * 
 */
public interface CustomAuthProvider extends AuthProvider {

  /**
   * Perform any needed initialization
   * 
   * @param properties
   *          the controller properties
   * @param vertx
   *          the current vertx instance
   */
  public void init(Properties properties, Vertx vertx);

}
