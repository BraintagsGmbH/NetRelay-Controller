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

import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.authentication.PasswordLostController;
import de.braintags.netrelay.controller.authentication.RegisterController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.util.MultipartUtil;
import de.braintags.vertx.auth.datastore.test.model.TestMemberEncrypted;
import de.braintags.vertx.jomnigate.IDataStore;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.testdatastore.DatastoreBaseTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TAuthenticationDatastoreEncrypted extends NetRelayBaseConnectorTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TAuthenticationDatastoreEncrypted.class);

  private static final String PROTECTED_URL = "/private/privatePage.html";
  private static final String TESTPASSWORD = "psspass";

  /**
   * Perform login and logout by resusing sent cookie
   */
  @Test
  public void loginLogout(TestContext context) {
    TestMemberEncrypted member = createMember(context, TESTPASSWORD);
    Buffer cookie = Buffer.buffer();
    try {
      resetRoutes(null);
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("username", member.getEmail());
      mu.addFormField("password", TESTPASSWORD);

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
   * @param context
   * @return
   */
  private TestMemberEncrypted createMember(TestContext context, String password) {
    TestMemberEncrypted member = new TestMemberEncrypted();
    member.setEmail("testuser");
    member.setPassword(password);
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
  private final TestMemberEncrypted createOrFindMember(TestContext context, IDataStore datastore,
      TestMemberEncrypted member) {
    IQuery<TestMemberEncrypted> query = datastore.createQuery(TestMemberEncrypted.class);
    String password = member.getPassword();
    context.assertNotNull(password, "password must not be null");
    query.setSearchCondition(ISearchCondition.isEqual("email", member.getEmail()));
    TestMemberEncrypted returnMember = (TestMemberEncrypted) DatastoreBaseTest.findFirst(context, query);
    if (returnMember == null) {
      DatastoreBaseTest.saveRecord(context, member);
      context.assertNotEquals(password, member.getPassword(), "password was not encrypted");
      returnMember = member;
    }
    return returnMember;
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

  private void improveRedirect(String redirectPath, TestContext context, ResponseCopy resp) {
    context.assertTrue(resp.headers.contains("location"), "parameter location does not exist");
    context.assertTrue(resp.headers.get("location").startsWith(redirectPath), "Expected redirect to " + redirectPath);
  }

  /**
   * "loginPage" : "/backend/login.html",
   * "logoutAction" : "/member/logout",
   * "logoutDestinationPage": "/backend/login.html",
   * "roleField" : "roles",
   * "collectionName" : "Member",
   * "loginAction" : "/member/login",
   * "authProvider" : "MongoAuth"
   * 
   * @throws Exception
   */
  private void resetRoutes(String directLoginPage) throws Exception {
    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(AuthenticationController.class.getSimpleName());
    if (directLoginPage != null) {
      def.getHandlerProperties().put(AuthenticationController.DIRECT_LOGGED_IN_OK_URL_PROP, directLoginPage);
    } else {
      def.getHandlerProperties().remove(AuthenticationController.DIRECT_LOGGED_IN_OK_URL_PROP);
    }
    netRelay.resetRoutes();
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

    RouterDefinition def = AuthenticationController.createDefaultRouterDefinition();
    def.getHandlerProperties().put(MongoAuth.PROPERTY_COLLECTION_NAME, "TestMemberEncrypted");
    def.setRoutes(new String[] { "/private/*" });
    def.getHandlerProperties().put(AuthenticationController.AUTH_PROVIDER_PROP,
        AuthenticationController.AUTH_PROVIDER_DATASTORE);
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), def);

    def = RegisterController.createDefaultRouterDefinition();
    def.getHandlerProperties().put(MongoAuth.PROPERTY_COLLECTION_NAME, "TestMemberEncrypted");
    settings.getRouterDefinitions().addAfter(AuthenticationController.class.getSimpleName(), def);

    settings.getRouterDefinitions().add(PasswordLostController.createDefaultRouterDefinition());
    settings.getMappingDefinitions().addMapperDefinition(Member.class);
    settings.getRouterDefinitions().addAfter(PasswordLostController.class.getSimpleName(), def);

    settings.getMappingDefinitions().addMapperDefinition(TestMemberEncrypted.class);

  }

}
