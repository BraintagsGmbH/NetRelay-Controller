/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.unit;

import java.util.List;

import org.junit.Test;

import de.braintags.netrelay.controller.CurrentMemberController;
import de.braintags.netrelay.controller.SessionController;
import de.braintags.netrelay.controller.ThymeleafTemplateController;
import de.braintags.netrelay.controller.api.MailController;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.authentication.RegisterController;
import de.braintags.netrelay.controller.authentication.RegistrationCode;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.model.RegisterClaim;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.util.MultipartUtil;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.testdatastore.DatastoreBaseTest;
import de.braintags.vertx.jomnigate.testdatastore.ResultContainer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.unit.TestContext;

/**
 *
 *
 * @author Michael Remme
 *
 */
public class TRegistration extends NetRelayBaseConnectorTest {
  /**
   * Comment for <code>MY_USERNAME</code>
   */
  private static final String MY_USERNAME = "myUsername";
  /**
   * Comment for <code>USER_BRAINTAGS_DE</code>
   */
  private static final String USER_BRAINTAGS_DE = "mremme@braintags.de";
  /**
   * Comment for <code>CUSTOMER_DO_CONFIRMATION</code>
   */
  private static final String CUSTOMER_DO_CONFIRMATION = "/customer/doConfirmation";
  public static final String REGISTER_URL = "/customer/doRegister";

  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TRegistration.class);

  @Test
  public void testRegisterCheckDirectLogin(TestContext context) {
    String email = USER_BRAINTAGS_DE;
    try {
      DatastoreBaseTest.clearTable(context, RegisterClaim.class);
      DatastoreBaseTest.clearTable(context, Member.class);
      resetRoutes();
      performRegistration(context, email);
      RegisterClaim claim = validateNoMultipleRequests(context, email);
      Buffer cookie = performConfirmation(context, claim);

      callProtectedPage(context, cookie);
    } catch (Exception e) {
      context.fail(e);
    }

  }

  private void callProtectedPage(TestContext context, Buffer cookie) throws Exception {
    String url = TAuthenticationMongo.PROTECTED_URL;
    testRequest(context, HttpMethod.POST, url, httpConn -> {
      httpConn.headers().set("Cookie", cookie.toString());
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      context.assertTrue(resp.content.contains("PRIVAT"), "protected page should be read, but was not");
    }, 200, "OK", null);
  }

  @Test
  public void testRegisterPasswordmissing(TestContext context) {
    String email = USER_BRAINTAGS_DE;
    try {
      DatastoreBaseTest.clearTable(context, RegisterClaim.class);
      DatastoreBaseTest.clearTable(context, Member.class);
      resetRoutes();
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField(RegisterController.EMAIL_FIELD_NAME, email);
      mu.addFormField(RegisterController.PASSWORD_FIELD_NAME, "");
      String url = REGISTER_URL;

      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains(RegistrationCode.PASSWORD_REQUIRED.toString()),
            "The error code is not set: " + RegistrationCode.PASSWORD_REQUIRED);
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void testRegister(TestContext context) {
    String email = USER_BRAINTAGS_DE;
    try {
      DatastoreBaseTest.clearTable(context, RegisterClaim.class);
      DatastoreBaseTest.clearTable(context, Member.class);
      resetRoutes();
      performRegistration(context, email);
      validateNoMultipleRequests(context, email);
      // perform second time for checking duplications
      performRegistration(context, email);
      RegisterClaim claim = validateNoMultipleRequests(context, email);
      performConfirmation(context, claim);
      performRegistrationExpectEmailExistsError(context, USER_BRAINTAGS_DE);
      // This must not create a second member
      performDuplicateConfirmation(context, claim);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  /**
   * @param context
   * @param email
   * @throws Exception
   */
  private Buffer performDuplicateConfirmation(TestContext context, RegisterClaim claim) throws Exception {
    Buffer cookie = Buffer.buffer();
    LOGGER.info("PERFORMING CONFIRMATION");
    String url = CUSTOMER_DO_CONFIRMATION + "?" + RegisterController.VALIDATION_ID_PARAM + "=" + claim.id;
    testRequest(context, HttpMethod.GET, url, req -> {

    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      context.assertTrue(resp.content.contains(RegistrationCode.CONFIRMATION_FAILURE.toString()),
          "The error code is not set: " + RegistrationCode.CONFIRMATION_FAILURE);
    }, 200, "OK", null);

    IQuery<Member> query = netRelay.getDatastore().createQuery(Member.class);
    query.setSearchCondition(ISearchCondition.isEqual(Member.EMAIL, USER_BRAINTAGS_DE));
    ResultContainer qres = DatastoreBaseTest.find(context, query, 1);
    context.assertNotNull(qres, "No result returned");
    Member member = (Member) DatastoreBaseTest.findFirst(context, query);
    context.assertNotNull(member, "Member was not created");
    context.assertEquals(MY_USERNAME, member.getUserName(), "username not set");
    return cookie;
  }

  /**
   * @param context
   * @param email
   * @throws Exception
   */
  private Buffer performConfirmation(TestContext context, RegisterClaim claim) throws Exception {
    Buffer cookie = Buffer.buffer();
    LOGGER.info("PERFORMING CONFIRMATION");
    String url = CUSTOMER_DO_CONFIRMATION + "?" + RegisterController.VALIDATION_ID_PARAM + "=" + claim.id;
    testRequest(context, HttpMethod.GET, url, req -> {

    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      context.assertNotNull(resp.headers.get("location"),
          "No location header set, check for an exception or an error page!");
      context.assertTrue(resp.headers.get("location").contains("/customer/registerConfirmSuccess.html"),
          "no final redirect on confirmation success");
      String setCookie = resp.headers.get("Set-Cookie");
      context.assertNotNull(setCookie, "Cookie not found");
      cookie.appendString(setCookie);
    }, 302, "Found", null);

    IQuery<Member> query = netRelay.getDatastore().createQuery(Member.class);
    query.setSearchCondition(ISearchCondition.isEqual(Member.EMAIL, USER_BRAINTAGS_DE));
    ResultContainer qres = DatastoreBaseTest.find(context, query, 1);
    context.assertNotNull(qres, "No result returned");
    Member member = (Member) DatastoreBaseTest.findFirst(context, query);
    context.assertNotNull(member, "Member was not created");
    context.assertEquals(MY_USERNAME, member.getUserName(), "username not set");
    return cookie;
  }

  /**
   * @param context
   * @param email
   * @throws Exception
   */
  private void performRegistrationExpectEmailExistsError(TestContext context, String email) throws Exception {
    String url = REGISTER_URL;
    MultipartUtil mu = createFormRequest(email);
    testRequest(context, HttpMethod.POST, url, req -> {
      mu.finish(req);
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      context.assertTrue(resp.content.contains(RegistrationCode.EMAIL_EXISTS.toString()),
          "The error code is not set: " + RegistrationCode.EMAIL_EXISTS);
    }, 200, "OK", null);
  }

  private MultipartUtil createFormRequest(String email) {
    MultipartUtil mu = new MultipartUtil();
    mu.addFormField(RegisterController.EMAIL_FIELD_NAME, email);
    mu.addFormField(RegisterController.PASSWORD_FIELD_NAME, "12345678");
    mu.addFormField("Member.userName", MY_USERNAME);
    return mu;
  }

  /**
   * @param context
   * @param email
   * @throws Exception
   */
  private void performRegistration(TestContext context, String email) throws Exception {
    MultipartUtil mu = createFormRequest(email);
    String url = REGISTER_URL;

    testRequest(context, HttpMethod.POST, url, req -> {
      mu.finish(req);
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      context.assertNotNull(resp.headers.get("location"), "No location header set");
      context.assertTrue(resp.headers.get("location").contains("/customer/registerSuccess.html"),
          "The success page isn't called");
    }, 302, "Found", null);
  }

  /**
   * validate, that there is only one active record with the email
   *
   * @param context
   * @param email
   * @return
   */
  private RegisterClaim validateNoMultipleRequests(TestContext context, String email) {
    IQuery<RegisterClaim> query = netRelay.getDatastore().createQuery(RegisterClaim.class);
    query.setSearchCondition(ISearchCondition.and(ISearchCondition.isEqual(RegisterClaim.EMAIL, email),
        ISearchCondition.isEqual(RegisterClaim.ACTIVE, true)));
    List<?> recList = DatastoreBaseTest.findAll(context, query);
    context.assertEquals(1, recList.size(), "previous RegisterClaims are not deactivated");
    return (RegisterClaim) recList.get(0);
  }

  private void resetRoutes() throws Exception {
    netRelay.resetRoutes();
  }

  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    initMailClient(settings);

    RouterDefinition def = AuthenticationController.createDefaultRouterDefinition();
    def.getHandlerProperties().put(MongoAuth.PROPERTY_COLLECTION_NAME, "Member");
    def.setRoutes(new String[] { "/private/*" });
    def.getHandlerProperties().put("collectionName", "Member");
    def.getHandlerProperties().put(AuthenticationController.AUTH_PROVIDER_PROP,
        AuthenticationController.AUTH_PROVIDER_DATASTORE);
    settings.getRouterDefinitions().addAfter(SessionController.class.getSimpleName(), def);

    RouterDefinition cmc = settings.getRouterDefinitions().remove(CurrentMemberController.class.getSimpleName());
    if (cmc == null) {
      cmc = new RouterDefinition();
      cmc.setActive(true);
      cmc.setController(CurrentMemberController.class);
    }
    cmc.setRoutes(new String[] { "/*" });
    settings.getRouterDefinitions().addBefore(AuthenticationController.class.getSimpleName(), cmc);

    def = RegisterController.createDefaultRouterDefinition();
    def.setRoutes(new String[] { REGISTER_URL, CUSTOMER_DO_CONFIRMATION });
    def.getHandlerProperties().put(RegisterController.REG_START_FAIL_URL_PROP, "/customer/registerError.html");
    def.getHandlerProperties().put(MailController.FROM_PARAM, TESTS_MAIL_FROM);
    def.getHandlerProperties().put(MailController.SUBJECT_PARAMETER, "Please finish registration");
    def.getHandlerProperties().put(MailController.TEMPLATE_PARAM, "/customer/confirmationMail.html");
    def.getHandlerProperties().put(ThymeleafTemplateController.TEMPLATE_DIRECTORY_PROPERTY, "testTemplates");
    def.getHandlerProperties().put("collectionName", "Member");
    def.getHandlerProperties().put("passwordField", "password");
    def.getHandlerProperties().put("usernameField", "email");
    def.getHandlerProperties().put("roleField", "roles");
    settings.getRouterDefinitions().addAfter(AuthenticationController.class.getSimpleName(), def);

    settings.getMappingDefinitions().addMapperDefinition(Member.class);

  }

}
