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
package de.braintags.netrelay.controller.authentication;

import java.util.Properties;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.vertx.auth.datastore.IAuthenticatable;
import de.braintags.vertx.auth.datastore.impl.DataStoreAuth;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.UserSessionHandler;

/**
 * 
 * This controller performs authentication ( login / logout ) and authorization ( role access, action access etc. ).
 * All routes, which are covered by this controller are protected. The controller takes automatically care about login
 * and logout of users.<br/>
 * If called, the controller stores the internal {@link AuthHandler} into the context under the property
 * "{@value #AUTH_HANDLER_PROP}", from where it can be called and reused by other Controllers. The
 * {@link PersistenceController}, for instance, calls the AuthHandler to clear the permissions of the current user on
 * the action(s), which shall be processed.<br/>
 * 
 * If a call to a protected page is performed, a 302-redirect to the defined login page is processed
 * If the login failed, then the controller tries to reroute the call to the defined login page again. Before it is
 * setting the parameter {@value #AUTHENTICATION_ERROR_PARAM}. If this page is not defined, then a 403 error is sent.
 * <br/>
 * 
 * Logout: performs logout and stores message in context with key LOGOUT_MESSAGE_PROP<br/>
 * 
 * Setting permissions:<br/>
 * Permissions can be defined by adding the property {@value #PERMISSIONS_PROP} to the configuration for the scope of
 * the current instance. The structure of one entry is defined like:<br/>
 * 
 * <pre>
 * permissionType: permissionDefinition; permissionType2: permissionData2
 * </pre>
 * 
 * As a concrete example, a role definition for the pure authorization for a template or a path could be:
 * 
 * <pre>
 * role: user, admin
 * </pre>
 * 
 * To define permissions on CRUD actions, a role definition can be extended like that:
 * 
 * <pre>
 * role: user{RU}, admin{CRUD}, *{R}
 * </pre>
 * 
 * This would give the same permissions to the pure template or path usage like above ( the * would not be used here).
 * Additionally, if a request would be processed by a PersistenceController as well, the PersistenceController would
 * check, wether all actions processed would be granted by this definition. The above definition would grant a
 * read/update right to the role user, Create / Read / Update / Delete rights to the role admin and READ right to any
 * group. If at least one action would not be covered by the used definition, the complete request would be rejected
 * with a 403 error.<br/>
 * <br/>
 * Config-Parameter:<br/>
 * <UL>
 * <LI>{@value #LOGIN_ACTION_URL_PROP}
 * <LI>{@value #LOGOUT_ACTION_URL_PROP}
 * <LI>{@value #LOGOUT_DESTINATION_PAGE_PROP}
 * <LI>{@value #DIRECT_LOGGED_IN_OK_URL_PROP}
 * <LI>{@value #LOGOUT_MESSAGE_PROP}
 * <LI>{@value #LOGIN_PAGE_PROP}
 * <LI>{@value #AUTH_HANDLER_PROP} - the name of the property, which defines the {@link AuthHandler} to be used.
 * Possible values are:
 * {@link AuthHandlerEnum#BASIC}, {@link AuthHandlerEnum#REDIRECT}
 * <LI>{@value #PERMISSIONS_PROP}
 * <LI>additionally add the properties of {@link AbstractAuthProviderController}
 * </UL>
 * <br>
 * Request-Parameter:<br/>
 * <br/>
 * Result-Parameter:<br/>
 * {@value #AUTHENTICATION_ERROR_PARAM} the parameter, where an error String of a failed authentication is stored in
 * the context
 * <br/>
 * 
 * Example configuration: <br/>
 * 
 * The configuration below protects the url /my-account/memberdata for users of any role. Users with the role "user" can
 * read and update records, users with the role "admin" can handle all actions on records and users with any other role
 * are only allowed to display records. +
 * This configuration makes use of {@link DataStoreAuth}, which uses the {@link IDataStore}, which is defined by
 * NetRelay. DatastoreAuth expects, that the instance to be handled ( and defined by the property "collectionName" ) is
 * an instance of {@link IAuthenticatable}. Because of that there is no need to define the properties usernameField,
 * passwordField and roleField. The login field is "email".
 * 
 * <pre>
    {
      "name" : "AuthenticationMemberdataController",
      "routes" : [ "/my-account/memberdata" ],
      "controller" : "de.braintags.netrelay.controller.authentication.AuthenticationController",
      "handlerProperties" : {
        "loginPage" : "/backend/login.html",
        "logoutAction" : "/member/logout",
        "collectionName" : "Member",
        "loginAction" : "/member/login",
        "authProvider" : "DatastoreAuth",
        "permissions" : "role: user{RU}, admin{CRUD}, *{R}"
      }
    }
 * 
 * </pre>
 * 
 * 
 * The configuration below protects all urls starting with /backend/system/ and /backend/dashboard/. Access is granted
 * for users with one of the roles marketing and admin, where marketing has the right to read and update records; admin
 * has the right to all actions. +
 * This configuration makes use of MongoAuth
 * 
 * <pre>
    {
      "name" : "AuthenticationBackendController",
      "routes" : [ "/backend/system/*", "/backend/dashboard/*" ],
      "controller" : "de.braintags.netrelay.controller.authentication.AuthenticationController",
      "handlerProperties" : {
        "loginPage" : "/backend/login.html",
        "logoutAction" : "/member/logout",
        "roleField" : "roles",
        "collectionName" : "Member",
        "loginAction" : "/member/login",
        "passwordField" : "password",
        "usernameField" : "userName",
        "authProvider" : "MongoAuth",
        "permissions" : "role: marketing{RU}, admin{CRUD}"
      }
    }
 * 
 * </pre>
 * 
 * @author mremme
 * 
 */
public class AuthenticationController extends AbstractAuthProviderController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AuthenticationController.class);

  /**
   * If an error occurs during login, then the error is added into the context. With this parameter you can define the
   * name of the parameter, by which it is added into the context.
   */
  public static final String AUTHENTICATION_ERROR_PARAM = "authenticationError";

  /**
   * With this property the url for the login can be defined. It is the url, which is called by a login form and is a
   * virtual url, where the {@link FormLoginHandler} is processed. Default is "/member/login"
   */
  public static final String LOGIN_ACTION_URL_PROP = "loginAction";

  /**
   * The url where a logout action is performed, if a member is logged in
   */
  public static final String LOGOUT_ACTION_URL_PROP = "logoutAction";

  /**
   * By this property you can define the destination page, which is called after a logout
   */
  public static final String LOGOUT_DESTINATION_PAGE_PROP = "logoutDestinationPage";

  /**
   * This is the name of the parameter, which defines the url to redirect to, if the user logs in directly at the url of
   * the form login handler without being redirected here first. ( parameter used by FormLoginHandler )
   */
  public static final String DIRECT_LOGGED_IN_OK_URL_PROP = "directLoggedInOKURL";

  /**
   * The default url, where the login action is performed
   */
  public static final String DEFAULT_LOGIN_ACTION_URL = "/member/login";

  /**
   * The default url, where the login action is performed
   */
  public static final String DEFAULT_LOGOUT_ACTION_URL = "/member/logout";

  /**
   * The default url which is called after a logout
   */
  public static final String DEFAULT_LOGOUT_DESTINATION = "/index.html";

  /**
   * When a logout was processed successfully and the page for a successfull logout is called, a
   * parameter is attached to that request, which will be then like:
   * logoutDestinationURL?{@value #LOGOUT_MESSAGE_PROP}=success;
   */
  public static final String LOGOUT_MESSAGE_PROP = "logoutMessage";

  /**
   * Defines the name of the property by which the {@link AuthHandler} to be used is defined inside the configuration
   * properties. Additionally this property name is used to store the instance of {@link AuthHandler} into the context,
   * from where it can be called from other Controllers, like the {@link PersistenceController} is doing when checking
   * the rights on a requested action
   */
  public static final String AUTH_HANDLER_PROP = "authHandler";

  /**
   * The name of the property which defines the login page to be used, something like "login.html", which is the page,
   * which contains the form, by which a user can enter his username and password
   */
  public static final String LOGIN_PAGE_PROP = "loginPage";

  /**
   * By this property the permissions can be defined, which are required inside the scope of the
   * AuthenticationController, which is defined by the routes.
   */
  public static final String PERMISSIONS_PROP = "permissions";

  protected AuthHandler authHandler;
  private String loginPage;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    context.put(AUTH_HANDLER_PROP, authHandler);
    MemberUtil.recoverContextUser(context);
    authHandler.handle(context);
  }

  @Override
  public void initProperties(Properties properties) {
    super.initProperties(properties);
    loginPage = (String) properties.get(LOGIN_PAGE_PROP);
    setupAuthentication(properties, getAuthProvider());
    initUserSessionHandler();
    initLoginAction();
    initLogoutAction();
    initPermissions();
  }

  /**
   * permissions are defined as csv list like
   * role:user, admin; {@value PersistenceController#PERMISSION_PROP_NAME}: DISPLAY, INSERT, UPDATE
   * 
   */
  private void initPermissions() {
    String permissions = readProperty(PERMISSIONS_PROP, null, false);
    if (permissions != null) {
      String[] perms = permissions.split(";");
      for (String perm : perms) {
        addOnePermissionType(perm);
      }
    }
  }

  private void addOnePermissionType(String permission) {
    String[] kv = permission.split(":");
    if (kv.length != 2) {
      throw new IllegalArgumentException("Wrong format of permission definition " + permission
          + ". The format must be permissionName: value1, value2");
    }
    String key = kv[0].trim();
    String[] vals = kv[1].split(",");
    for (String val : vals) {
      authHandler.addAuthority(key + ":" + val.trim());
    }
  }

  private void initUserSessionHandler() {
    getNetRelay().getRouter().route().handler(UserSessionHandler.create(getAuthProvider()));
  }

  private void initLogoutAction() {
    String logoutUrl = readProperty(LOGOUT_ACTION_URL_PROP, DEFAULT_LOGOUT_ACTION_URL, false);
    String logoutDestinationURL = readProperty(LOGOUT_DESTINATION_PAGE_PROP, DEFAULT_LOGOUT_DESTINATION, false);
    getNetRelay().getRouter().route(logoutUrl).handler(context -> {
      MemberUtil.logout(context);
      String path = logoutDestinationURL + "?" + LOGOUT_MESSAGE_PROP + "=success";
      RequestUtil.sendRedirect(context.response(), path);
    });

  }

  private void initLoginAction() {
    String loginUrl = readProperty(LOGIN_ACTION_URL_PROP, DEFAULT_LOGIN_ACTION_URL, false);
    String directLoginUrl = readProperty(DIRECT_LOGGED_IN_OK_URL_PROP, null, false);
    String authErrorParam = readProperty(AUTHENTICATION_ERROR_PARAM, null, false);

    FormLoginHandlerBt fl = new FormLoginHandlerBt(getAuthProvider());
    if (directLoginUrl != null) {
      fl.setDirectLoggedInOKURL(directLoginUrl);
    }
    if (loginPage != null) {
      fl.setLoginPage(loginPage);
    }
    if (authErrorParam != null) {
      fl.setAuthenticationErrorParameter(authErrorParam);
    }
    getNetRelay().getRouter().route(loginUrl).handler(fl);
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(AuthenticationController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(AuthenticationController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/member/*" });
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(LOGIN_PAGE_PROP, "/member/login");
    json.put(AUTH_PROVIDER_PROP, AUTH_PROVIDER_DATASTORE);
    json.put(MongoAuth.PROPERTY_COLLECTION_NAME, "usertable");
    json.put(LOGIN_ACTION_URL_PROP, DEFAULT_LOGIN_ACTION_URL);
    json.put(LOGOUT_ACTION_URL_PROP, DEFAULT_LOGOUT_ACTION_URL);
    json.put(LOGOUT_DESTINATION_PAGE_PROP, DEFAULT_LOGOUT_DESTINATION);
    return json;
  }

  private void setupAuthentication(Properties properties, AuthProvider authProvider) {
    AuthHandlerEnum ae = AuthHandlerEnum.valueOf(readProperty(AUTH_HANDLER_PROP, "REDIRECT", false));
    switch (ae) {
    case BASIC:
      authHandler = BasicAuthHandler.create(authProvider);
      break;

    case REDIRECT:
      authHandler = new RedirectAuthHandlerBt(authProvider, loginPage, RedirectAuthHandler.DEFAULT_RETURN_URL_PARAM);
      break;

    default:
      throw new UnsupportedOperationException("unsupported definition for authentication handler: " + ae);
    }
  }

  public enum AuthHandlerEnum {
    /**
     * Used as possible value for {@link AbstractAuthController#AUTH_HANDLER_PROP} and creates a
     * {@link BasicAuthHandler}
     */
    BASIC(),
    /**
     * Used as possible value for {@link AbstractAuthController#AUTH_HANDLER_PROP} and creates a
     * {@link RedirectAuthHandler}
     */
    REDIRECT();
  }
}
