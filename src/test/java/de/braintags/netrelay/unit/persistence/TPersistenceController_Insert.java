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
package de.braintags.netrelay.unit.persistence;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.pojomapper.testdatastore.mapper.SimpleMapper;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.controller.persistence.RecordContractor;
import de.braintags.netrelay.impl.NetRelayExt_FileBasedSettings;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.City;
import de.braintags.netrelay.model.Country;
import de.braintags.netrelay.model.Street;
import de.braintags.netrelay.model.TestCustomer;
import de.braintags.netrelay.model.TestPhone;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractPersistenceControllerTest;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.test.core.TestUtils;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TPersistenceController_Insert extends AbstractPersistenceControllerTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TPersistenceController_Insert.class);
  private static final String INSERT_CUSTOMER_URL = "/customer/insertCustomer.html";
  private static final String INSERT_CITY_URL = "/country/insertCity.html";

  @Test
  public void testInsertSubSubObject(TestContext context) throws Exception {
    CheckController.checkMapperName = Country.class.getSimpleName();
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(Country.class);
    IMapper cityMapper = netRelay.getDatastore().getMapperFactory().getMapper(City.class);
    Country tmpCountry = initCountry(context);
    String street = "Karl-August-Strasse";

    try {
      // insert.html?entity=Country{ID:1}.cities{ID:3}.streets&action=INSERT
      String entityDef = RecordContractor.generateEntityReference(mapper, tmpCountry);
      City city = tmpCountry.cities.get(0);
      entityDef += ".cities" + RecordContractor.createIdReference(cityMapper, city) + ".streets";

      String url = String.format(INSERT_CITY_URL + "?action=INSERT&entity=%s", entityDef);
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("Country.cities.streets.name", street);
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("Germany"), "Expected name not found in response");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

    // after this request the customer must contain the phone-number
    Country savedCountry = (Country) DatastoreBaseTest.findRecordByID(context, Country.class, tmpCountry.id);
    context.assertEquals(1, savedCountry.cities.size(), "Expected one city");
    context.assertEquals(2, savedCountry.cities.get(0).streets.size(), "Expected two streets");

    boolean found = false;
    for (Street str : savedCountry.cities.get(0).streets) {
      if (str.name.equals(street)) {
        found = true;
      }
    }
    context.assertTrue(found, "new street was not saved");
  }

  /**
   * Insert subobject by form
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testInsertSubObject(TestContext context) throws Exception {
    String newNumber = "222232323";
    CheckController.checkMapperName = TestCustomer.class.getSimpleName();
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(TestCustomer.class);
    TestCustomer customer = initCustomer(context);
    Object id = customer.getId();
    LOGGER.info("ID: " + id);

    try {
      // insert.html?entity=Person{ID:1}.phoneNumbers&action=INSERT
      String entityDef = RecordContractor.generateEntityReference(mapper, customer);
      String url = String.format(INSERT_CUSTOMER_URL + "?action=INSERT&entity=%s", entityDef + ".phoneNumbers");
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("TestCustomer.phoneNumbers.phoneNumber", newNumber);
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("testcustomer"), "Expected name not found in response");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

    // after this request the customer must contain the phone-number
    TestCustomer savedCustomer = (TestCustomer) DatastoreBaseTest.findRecordByID(context, TestCustomer.class,
        customer.getId());
    context.assertEquals(2, savedCustomer.getPhoneNumbers().size(), "Expected two phone numbers");
    boolean found = false;
    for (TestPhone phone : savedCustomer.getPhoneNumbers()) {
      if (phone.getPhoneNumber().equals(newNumber)) {
        found = true;
      }
    }
    context.assertTrue(found, "new phone number was not saved");
    for (TestPhone phone : savedCustomer.getPhoneNumbers()) {
      if (phone.id == null) {
        context.fail("The id of a phone number was not set");
        break;
      }
    }
  }

  @Test
  public void testInsertAsParameter(TestContext context) throws Exception {
    try {
      CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
      String url = String.format("/products/insert2.html?action=INSERT&entity=%s",
          NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME);
      MultipartUtil mu = new MultipartUtil();
      addFields(mu);
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("myFirstName"), "Expected name not found in response");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void testInsertAsParameterWithFile(TestContext context) throws Exception {
    try {
      CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
      String url = String.format("/products/insert3.html?action=INSERT&entity=%s",
          NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME);
      MultipartUtil mu = new MultipartUtil();
      addFields(mu);
      LOGGER.info("UploadDirectory: " + BodyHandler.DEFAULT_UPLOADS_DIRECTORY);
      String fieldName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME + ".fileName";
      String fileName = "somefile.dat";
      String contentType = "application/octet-stream";
      Buffer fileData = TestUtils.randomBuffer(50);
      mu.addFilePart(fieldName, fileName, contentType, fileData);

      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        String response = resp.content.toString();
        context.assertTrue(response.contains("myFirstName"), "Expected name not found in response");
        // do not search for real filename, because of numeration for deduplication
        context.assertTrue(response.contains("somefile"), "Expected filename not fount in response");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void testInsertAsCapture(TestContext context) throws Exception {
    try {
      CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
      String url = String.format("/products/%s/INSERT/insert.html", NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME);
      MultipartUtil mu = new MultipartUtil();
      addFields(mu);
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("myFirstName"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  /**
   * @param mu
   */
  protected void addFields(MultipartUtil mu) {
    mu.addFormField("origin", "junit-testUserAlias");
    mu.addFormField("login", "admin@foo.bar");
    mu.addFormField("pass word", "admin");
    mu.addFormField(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME + ".name", "myFirstName");
    mu.addFormField(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME + ".age", "18");
    mu.addFormField(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME + ".child", "true");
    mu.addFormField(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME + ".geoPoint", "[52.666, 78,999]");
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.NetRelayBaseTest#modifySettings(de.braintags.netrelay.init.Settings)
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    RouterDefinition persistenceDefinition = PersistenceController.createDefaultRouterDefinition();
    persistenceDefinition.setRoutes(new String[] { "/products/:entity/:action/insert.html", "/products/insert2.html",
        "/products/insert3.html", INSERT_CUSTOMER_URL, INSERT_CITY_URL });
    persistenceDefinition.getHandlerProperties().put(PersistenceController.UPLOAD_DIRECTORY_PROP,
        "webroot/images/productImages");
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), persistenceDefinition);
    setPersistenceDef(persistenceDefinition);

    RouterDefinition rd = new RouterDefinition();
    rd.setControllerClass(CheckController.class);
    CheckController.checkMapperName = SimpleMapper.class.getSimpleName();
    settings.getRouterDefinitions().addAfter(PersistenceController.class.getSimpleName(), rd);
  }

}
