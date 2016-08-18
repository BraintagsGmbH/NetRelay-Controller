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

import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.pojomapper.testdatastore.ResultContainer;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.impl.NetRelayExt_FileBasedSettings;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
import de.braintags.netrelay.model.Member;
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
public class TAuthorization extends NetRelayBaseConnectorTest {

  public static final String PROTECTED_URL = "/private/privatePage.html";
  public static final String PROTECTED_PERSISTENCE_URL = "/private/persistence/privatePage.html";
  protected RouterDefinition persistenceDefinition;

  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TAuthorization.class);

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasNoUpdatePermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceNOK(context, member, mapper, "role: admin{U}", PROTECTED_PERSISTENCE_URL, Action.UPDATE);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasUpdatePermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceOK(context, member, mapper, "role: admin{U}", PROTECTED_PERSISTENCE_URL, Action.UPDATE);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasNoDeletePermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceNOK(context, member, mapper, "role: admin{D}", PROTECTED_PERSISTENCE_URL, Action.DISPLAY);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasDeletePermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceOK(context, member, mapper, "role: admin{D}", PROTECTED_PERSISTENCE_URL, Action.DELETE);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasNoReadPermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceNOK(context, member, mapper, "role: admin{R}", PROTECTED_PERSISTENCE_URL, Action.DISPLAY);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasReadPermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    SimpleNetRelayMapper mapper = createInstance(context, true);
    testExpectsPersistenceOK(context, member, mapper, "role: admin{R}", PROTECTED_PERSISTENCE_URL, Action.DISPLAY);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasWildcardPermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsPersistenceOK(context, member, null, "role: *{C}", PROTECTED_PERSISTENCE_URL, Action.INSERT);
  }

  /**
   * A call to a template is performed to insert a record and the template has not defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_CheckTwoMembers(TestContext context) throws Exception {
    Member admin = createMember(context, true, "TestUser3", "admin", "users");
    Member user = createMember(context, false, "TestUser4", "users");
    testExpectsPersistenceOK(context, admin, null, "role: admin{C}, users, bookers", PROTECTED_PERSISTENCE_URL,
        Action.INSERT);
    testExpectsPersistenceNOK(context, user, null, "role: admin{C}, users, bookers", PROTECTED_PERSISTENCE_URL,
        Action.INSERT);
  }

  /**
   * A call to a template is performed to insert a record and the template has not defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasNoInsertPermission(TestContext context) throws Exception {
    Member admin = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsPersistenceNOK(context, admin, null, "role: admin, users, bookers", PROTECTED_PERSISTENCE_URL,
        Action.INSERT);
  }

  /**
   * A call to a template is performed to insert a record and the template has defined this permission
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_HasInsertPermission(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsPersistenceOK(context, member, null, "role: admin{C}, users, bookers", PROTECTED_PERSISTENCE_URL,
        Action.INSERT);
  }

  /**
   * Wildcard role is set, expecting ok
   * We are expecting OK
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_Wildcard(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsOK(context, member, "role: *");
  }

  /**
   * role admin is required, user has it
   * We are expecting OK
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_UserHasRole3(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsOK(context, member, "role: admin, users, bookers");
  }

  /**
   * role admin is required, user has no role
   * We are expecting OK
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_UserHasRole2(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin", "users");
    testExpectsOK(context, member, "role: admin");
  }

  /**
   * role admin is required, user has no role
   * We are expecting OK
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_UserHasRole(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser3", "admin");
    testExpectsOK(context, member, "role: admin");
  }

  /**
   * role admin is required, user has no role
   * We are expecting status 403 - forbidden
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testRole_UserNoRole(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser2", null);
    testExpectsForbidden(context, member, "role: admin");
  }

  /**
   * No permissions required and user has no roles, but protected by Authentication
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testNoPermissions(TestContext context) throws Exception {
    Member member = createMember(context, true, "TestUser1", null);
    testExpectsOK(context, member, null);
  }

  /**
   * Test expects that permissions and user rights are NOT fitting, so that user has NO right for the request
   * 
   * @param context
   * @throws Exception
   */
  public void testExpectsForbidden(TestContext context, Member member, String permissions) throws Exception {
    resetRoutes(permissions);
    String cookie = login(context, member);
    if (cookie != null) {
      String url = PROTECTED_URL;
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        httpConn.headers().set("Cookie", cookie.toString());
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
      }, 403, "Forbidden", null);
    }
  }

  /**
   * Test expects that permissions and user rights are fitting, so that user has the right for the request
   * 
   * @param context
   * @throws Exception
   */
  public void testExpectsPersistenceNOK(TestContext context, Member member, SimpleNetRelayMapper mapper,
      String templatePermissions, String url, Action action) throws Exception {
    resetRoutes(templatePermissions);
    if (action.equals(Action.INSERT)) {
      url = String.format(url + "?action=%s&entity=%s", "INSERT", NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME);
    } else {
      url = url + "?" + AbstractPersistenceControllerTest.createReferenceAsParameter(context, persistenceDefinition,
          action, mapper);
    }
    String cookie = login(context, member);
    if (cookie != null) {
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        httpConn.headers().set("Cookie", cookie.toString());
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNull(setCookie, "Cookie should not be sent here");
      }, 403, "Forbidden", null);
    } else {
      context.fail("Expected a cookie here");
    }
  }

  /**
   * Test expects that permissions and user rights are fitting, so that user has the right for the request
   * 
   * @param context
   * @throws Exception
   */
  public void testExpectsPersistenceOK(TestContext context, Member member, SimpleNetRelayMapper mapper,
      String templatePermissions, String url, Action action) throws Exception {
    resetRoutes(templatePermissions);
    if (action.equals(Action.INSERT)) {
      url = String.format(url + "?action=%s&entity=%s", "INSERT", NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME);
    } else {
      url = url + "?" + AbstractPersistenceControllerTest.createReferenceAsParameter(context, persistenceDefinition,
          action, mapper);
    }
    String cookie = login(context, member);
    if (cookie != null) {
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        httpConn.headers().set("Cookie", cookie.toString());
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains("PRIVAT"), "protected page should be read, but was not");
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNull(setCookie, "Cookie should not be sent here");
      }, 200, "OK", null);
    } else {
      context.fail("Expected a cookie here");
    }
  }

  /**
   * Test expects that permissions and user rights are fitting, so that user has the right for the request
   * 
   * @param context
   * @throws Exception
   */
  public void testExpectsOK(TestContext context, Member member, String permissions) throws Exception {
    resetRoutes(permissions);
    String cookie = login(context, member);
    if (cookie != null) {
      String url = PROTECTED_URL;
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        httpConn.headers().set("Cookie", cookie.toString());
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains("PRIVAT"), "protected page should be read, but was not");
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNull(setCookie, "Cookie should not be sent here");
      }, 200, "OK", null);
    } else {
      context.fail("Expected a cookie here");
    }
  }

  /**
   * Perform login and return cookie
   */
  public String login(TestContext context, Member member) {
    Buffer cookie = Buffer.buffer();
    try {
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("username", member.getUserName());
      mu.addFormField("password", member.getPassword());

      // first perform the login and remember cookie
      String url = AuthenticationController.DEFAULT_LOGIN_ACTION_URL;
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        context.assertTrue(resp.content.contains("Login successful"), "required text in reply not found");
        String setCookie = resp.headers.get("Set-Cookie");
        context.assertNotNull(setCookie, "Cookie not found");
        cookie.appendString(setCookie);
      }, 200, "OK", null);
      return cookie.toString();
    } catch (Exception e) {
      context.fail(e);
      return null;
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
  private void resetRoutes(String permissions) throws Exception {
    RouterDefinition def1 = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(AuthenticationController.class.getSimpleName());
    def1.setRoutes(new String[] { "/private/*" });
    def1.getHandlerProperties().put("collectionName", "Member");
    def1.getHandlerProperties().put("passwordField", "password");
    def1.getHandlerProperties().put("usernameField", "userName");
    def1.getHandlerProperties().put("roleField", "roles");
    if (permissions != null) {
      def1.getHandlerProperties().put(AuthenticationController.PERMISSIONS_PROP, permissions);
    } else {
      def1.getHandlerProperties().remove(AuthenticationController.PERMISSIONS_PROP);
    }

    persistenceDefinition = netRelay.getSettings().getRouterDefinitions()
        .remove(PersistenceController.class.getSimpleName());
    persistenceDefinition.setRoutes(new String[] { PROTECTED_PERSISTENCE_URL });
    netRelay.getSettings().getRouterDefinitions().addAfter(AuthenticationController.class.getSimpleName(),
        persistenceDefinition);
    netRelay.resetRoutes();
  }

  /**
   * @param context
   * @return
   */
  private Member createMember(TestContext context, boolean clearTable, String username, String... roles) {
    if (clearTable) {
      DatastoreBaseTest.clearTable(context, Member.class);
    }
    Member member = new Member();
    member.setUserName(username);
    member.setPassword("testpassword");
    if (roles != null) {
      for (String role : roles) {
        member.getRoles().add(role);
      }
    }
    member = createOrFindMember(context, netRelay.getDatastore(), member);
    context.assertNotNull(member, "Member must not be null");
    return member;
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
    settings.getMappingDefinitions().addMapperDefinition(Member.class);
  }

  private SimpleNetRelayMapper createInstance(TestContext context, boolean resetTable) {
    if (resetTable) {
      DatastoreBaseTest.clearTable(context, SimpleNetRelayMapper.class);
    }
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    ResultContainer cont = DatastoreBaseTest.saveRecord(context, mapper);
    return mapper;
  }

}
