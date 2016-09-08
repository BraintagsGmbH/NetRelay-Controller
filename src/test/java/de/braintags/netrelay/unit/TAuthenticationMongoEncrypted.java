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
import de.braintags.netrelay.controller.authentication.RegisterController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.vertx.auth.datastore.test.model.TestMemberEncrypted;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TAuthenticationMongoEncrypted extends NetRelayBaseConnectorTest {
  /**
   * Comment for <code>TESTPASSWORD</code>
   */
  public static final String TESTPASSWORD = "psspass";
  /**
   * Comment for <code>PROTECTED_URL</code>
   */
  public static final String PROTECTED_URL = "/private/privatePage.html";
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TAuthenticationMongoEncrypted.class);

  public static boolean notSupported = false;

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.unit.NetRelayBaseTest#initNetRelay(io.vertx.ext.unit.TestContext)
   */
  @Override
  public void initNetRelay(TestContext context) {
    try {
      super.initNetRelay(context);
    } catch (Exception e) {
      LOGGER.info(e);
      notSupported = true;
    }
  }

  /**
   * Improves that for a call to a protected page a redirect is sent
   * 
   * @param context
   */
  @Test
  public void testNotSupported(TestContext context) {
    context.assertTrue(notSupported);
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
    def.setRoutes(new String[] { "/private/*" });
    def.getHandlerProperties().put("collectionName", "TestMemberEncrypted");
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
  public static TestMemberEncrypted createMember(TestContext context, String password) {
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
  public static final TestMemberEncrypted createOrFindMember(TestContext context, IDataStore datastore,
      TestMemberEncrypted member) {
    IQuery<TestMemberEncrypted> query = datastore.createQuery(TestMemberEncrypted.class);
    String password = member.getPassword();
    context.assertNotNull(password, "password must not be null");
    query.field("email").is(member.getEmail());
    TestMemberEncrypted returnMember = (TestMemberEncrypted) DatastoreBaseTest.findFirst(context, query);
    if (returnMember == null) {
      ResultContainer cont = DatastoreBaseTest.saveRecord(context, member);
      context.assertNotEquals(password, member.getPassword(), "password was not encrypted");
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

    RouterDefinition def = settings.getRouterDefinitions()
        .getNamedDefinition(AuthenticationController.class.getSimpleName());
    def.setRoutes(new String[] { "/private/*" });
    def.getHandlerProperties().put("collectionName", "TestMemberEncrypted");

    def = settings.getRouterDefinitions().getNamedDefinition(RegisterController.class.getSimpleName());
    def.getHandlerProperties().put("collectionName", "TestMemberEncrypted");

    settings.getMappingDefinitions().addMapperDefinition(TestMemberEncrypted.class);
  }

}
