package de.braintags.netrelay.controller.authentication.loginhandler;

import java.util.Properties;

import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.RedirectAuthHandler;

/**
 * Abstract implementation of the {@link LoginHandler} interface. Passes the data from the implementation to the
 * auth-provider, and executes the following steps after the authentication:
 * <p>
 * On success:
 * </p>
 * <ul>
 * <li>redirect to a custom URL given at request</li>
 * <li>if not given, redirect to a URL saved inside the session</li>
 * <li>if no URL found, redirect to a static configured URL</li>
 * <li>if not configured, return a basic "Login successful" page</li>
 * </ul>
 * 
 * <p>
 * On failure:
 * </p>
 * either redirect back to the login page, or if the user was already on the login page, return a 403 error.<br/>
 * <br/>
 * 
 * @author sschmitt
 * 
 */
public abstract class AbstractLoginHandler implements LoginHandler {

  public static final String DEFAULT_AUTHENTICATION_ERROR_PARAM = "authenticationError";

  private static final String DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = ""
      + "<html><body><h1>Login successful</h1></body></html>";

  private AuthProvider authProvider;

  private String authenticationErrorParameter;
  private String loginPage;

  protected String directLoggedInOKURL;
  protected String returnURLParam;

  private boolean initialized = false;

  @Override
  public void init(AuthProvider authProvider, Properties properties) {
    this.authProvider = authProvider;
    this.authenticationErrorParameter = properties.getProperty(AuthenticationController.AUTHENTICATION_ERROR_PARAM,
        DEFAULT_AUTHENTICATION_ERROR_PARAM);
    this.loginPage = properties.getProperty(AuthenticationController.LOGIN_PAGE_PROP);
    this.directLoggedInOKURL = properties.getProperty(AuthenticationController.DIRECT_LOGGED_IN_OK_URL_PROP, null);
    this.returnURLParam = properties.getProperty(AuthenticationController.RETURN_URL_PARAM_PROP,
        RedirectAuthHandler.DEFAULT_RETURN_URL_PARAM);
    initialized = true;
  }

  protected void authenticate(JsonObject authInfo, String customRedirectUrl, RoutingContext context) {
    authProvider.authenticate(authInfo, userResult -> {
      if (userResult.succeeded()) {
        User user = userResult.result();
        MemberUtil.setContextUser(context, user);
        if (!redirectByCustomUrl(customRedirectUrl, context) && !redirectBySession(context)
            && !redirectByDirectLoginUrl(context)) {
          // Just show a basic page
          context.request().response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE);
        }
      } else {
        handleAuthenticationError(context, userResult.cause());
      }
    });
  }

  private boolean redirectByCustomUrl(String customRedirectUrl, RoutingContext context) {
    if (customRedirectUrl != null) {
      RequestUtil.sendRedirect(context, customRedirectUrl);
      return true;
    }
    return false;
  }

  private void handleAuthenticationError(RoutingContext context, Throwable e) {
    if (e != null) {
      context.put(authenticationErrorParameter, e.toString());
    }
    String currentPage = context.request().path();
    if (loginPage != null && !currentPage.equals(loginPage)) {
      context.reroute(loginPage);
    } else {
      context.fail(403);
    }
  }

  private boolean redirectBySession(RoutingContext context) {
    if (context.session() != null) {
      String returnURL = context.session().remove(returnURLParam);
      if (returnURL != null) {
        // Now redirect back to the original url
        RequestUtil.sendRedirect(context, returnURL);
        return true;
      }
    }
    return false;
  }

  private boolean redirectByDirectLoginUrl(RoutingContext context) {
    if (directLoggedInOKURL != null) {
      // Redirect to the default logged in OK page - this would occur
      // if the user logged in directly at this URL without being redirected here first from another
      // url
      RequestUtil.sendRedirect(context, directLoggedInOKURL);
      return true;
    }
    return false;
  }

  public boolean isInitialized() {
    return initialized;
  }

}
