package de.braintags.netrelay.controller.authentication.loginhandler;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Login handler that takes the whole body of a request and passes it onto the auth-provider. A custom parameter name
 * can be used to extract a redirect url from the body that will be used to redirect the user on successful login.
 * 
 * @author sschmitt
 * 
 */
public class JsonBodyLoginHandler extends AbstractLoginHandler {

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    if (!isInitialized())
      throw new IllegalStateException("Can not handle request, this login handler was not initialized");

    HttpServerRequest req = context.request();
    if (req.method() != HttpMethod.POST) {
      context.fail(405); // Must be a POST
    } else {
      JsonObject authInfo = context.getBodyAsJson();
      String customReturnUrl = authInfo.getString(returnURLParam);
      authenticate(authInfo, customReturnUrl, context);
    }
  }
}
