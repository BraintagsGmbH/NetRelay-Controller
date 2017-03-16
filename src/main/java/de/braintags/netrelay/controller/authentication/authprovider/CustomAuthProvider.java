package de.braintags.netrelay.controller.authentication.authprovider;

import java.util.Properties;

import de.braintags.netrelay.NetRelay;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
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
   * @param netRelay
   *          the current NetRelay instance
   */
  public void init(Properties properties, NetRelay netRelay);

}
