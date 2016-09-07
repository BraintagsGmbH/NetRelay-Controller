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
package de.braintags.netrelay.unit;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.pojomapper.testdatastore.ResultContainer;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.MemberEncrypted;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TAuthenticationEncrypted extends NetRelayBaseConnectorTest {
  /**
   * Comment for <code>TESTPASSWORD</code>
   */
  private static final String TESTPASSWORD = "psspass";
  /**
   * Comment for <code>PROTECTED_URL</code>
   */
  public static final String PROTECTED_URL = "/private/privatePage.html";
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TAuthenticationEncrypted.class);

  /**
   * Perform login and logout by resusing sent cookie
   */
  @Test
  public void loginLogout(TestContext context) {
    MemberEncrypted member = createMember(context);
    Buffer cookie = Buffer.buffer();
    try {
      resetRoutes(null);
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("username", member.getUserName());
      mu.addFormField("password", member.passwordEnc);

      // first perform the login and remember cookie
      String url = AuthenticationController.DEFAULT_LOGIN_ACTION_URL;
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains("Login successful"), "required text in reply not found");
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNotNull(setCookie, "Cookie not found");
        cookie.appendString(setCookie);
      }, 200, "OK", null);

      // second call protected page and set cookie
      url = PROTECTED_URL;
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        httpConn.headers().set("Cookie", cookie.toString());
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains("PRIVAT"), "protected page should be read, but was not");
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNull(setCookie, "Cookie should not be sent here");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  /**
   * Improves that for a call to a protected page a redirect is sent
   * 
   * @param context
   */
  @Test
  public void testSimpleLogin(TestContext context) {
    try {
      resetRoutes(null);
      String url = PROTECTED_URL;
      testRequest(context, HttpMethod.POST, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        improveRedirect(AuthenticationController.DEFAULT_LOGIN_ACTION_URL, context, resp);
      }, 302, "Found", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  /**
   * "loginPage" : "/backend/login.html",
   * "logoutAction" : "/member/logout",
   * "logoutDestinationPage": "/backend/login.html",
   * "roleField" : "roles",
   * "collectionName" : "Member",
   * "loginAction" : "/member/login",
   * "passwordField" : "password",
   * "usernameField" : "userName",
   * "authProvider" : "MongoAuth"
   * 
   * @throws Exception
   */
  private void resetRoutes(String directLoginPage) throws Exception {
    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(AuthenticationController.class.getSimpleName());
    def.setRoutes(new String[] { "/private/*" });
    def.getHandlerProperties().put("collectionName", "MemberEncrypted");
    def.getHandlerProperties().put("passwordField", "passwordEnc");
    def.getHandlerProperties().put("usernameField", "userName");
    def.getHandlerProperties().put("roleField", "roles");
    if (directLoginPage != null) {
      def.getHandlerProperties().put(AuthenticationController.DIRECT_LOGGED_IN_OK_URL_PROP, directLoginPage);
    } else {
      def.getHandlerProperties().remove(AuthenticationController.DIRECT_LOGGED_IN_OK_URL_PROP);
    }
    netRelay.resetRoutes();
  }

  /**
   * @param context
   * @return
   */
  private MemberEncrypted createMember(TestContext context) {
    MemberEncrypted member = new MemberEncrypted();
    member.setUserName("testuser");
    member.passwordEnc = TESTPASSWORD;
    member.setPassword("xxx");
    member = createOrFindMember(context, netRelay.getDatastore(), member);
    context.assertNotNull(member, "Member must not be null");
    return member;
  }

  /**
   * Searches in the database, wether a member with the given username / password exists.
   * If not, it is created. After the found or created member is returned
   * 
   * @param context
   * @param datastore
   * @param member
   * @return
   */
  public static final MemberEncrypted createOrFindMember(TestContext context, IDataStore datastore,
      MemberEncrypted member) {
    IQuery<MemberEncrypted> query = datastore.createQuery(MemberEncrypted.class);
    String password = member.passwordEnc;
    query.field("userName").is(member.getUserName());
    MemberEncrypted returnMember = (MemberEncrypted) DatastoreBaseTest.findFirst(context, query);
    if (returnMember == null) {
      ResultContainer cont = DatastoreBaseTest.saveRecord(context, member);
      context.assertNotEquals(password, member.passwordEnc, "password was not encrypted");
      returnMember = member;
    }
    return returnMember;
  }

  private void improveRedirect(String redirectPath, TestContext context, ResponseCopy resp) {
    context.assertTrue(resp.headers.contains("location"), "parameter location does not exist");
    context.assertTrue(resp.headers.get("location").startsWith(redirectPath), "Expected redirect to " + redirectPath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.unit.NetRelayBaseConnectorTest#modifySettings(io.vertx.ext.unit.TestContext,
   * de.braintags.netrelay.init.Settings)
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    settings.getMappingDefinitions().addMapperDefinition(MemberEncrypted.class);
  }

}
