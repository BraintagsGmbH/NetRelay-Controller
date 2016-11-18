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
import de.braintags.io.vertx.pojomapper.testdatastore.ResultContainer;
import de.braintags.io.vertx.pojomapper.testdatastore.mapper.SimpleMapper;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.controller.persistence.RecordContractor;
import de.braintags.netrelay.impl.NetRelayExt_FileBasedSettings;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
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

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TPersistenceController_Update extends AbstractPersistenceControllerTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TPersistenceController_Update.class);

  private static final String UPDATE_CUSTOMER_URL = "/customer/updateCustomer.html";
  private static final String UPDATE_CITY_URL = "/country/updateCity.html";

  @Test
  public void testUpdateSubRecord(TestContext context) {
    String newNumber = "222232323";
    CheckController.checkMapperName = TestCustomer.class.getSimpleName();
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(TestCustomer.class);
    IMapper phoneMapper = netRelay.getDatastore().getMapperFactory().getMapper(TestPhone.class);
    TestCustomer customer = initCustomer(context);
    Object id = customer.getId();
    LOGGER.info("ID: " + id);

    try {
      // insert.html?entity=Person{ID:1}.phoneNumbers&action=INSERT
      String entityDef = RecordContractor.generateEntityReference(mapper, customer);
      entityDef += ".phoneNumbers" + RecordContractor.createIdReference(phoneMapper, customer.getPhoneNumbers().get(0));

      String url = String.format(UPDATE_CUSTOMER_URL + "?action=UPDATE&entity=%s", entityDef);
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
    context.assertEquals(1, savedCustomer.getPhoneNumbers().size(), "Expected two phone numbers");
    boolean found = false;
    for (TestPhone phone : savedCustomer.getPhoneNumbers()) {
      if (phone.getPhoneNumber().equals(newNumber)) {
        found = true;
      }
    }
    context.assertTrue(found, "modified phone number was not saved");
    for (TestPhone phone : savedCustomer.getPhoneNumbers()) {
      if (phone.id == null) {
        context.fail("The id of a phone number was not set");
        break;
      }
    }
  }

  @Test
  public void testUpdateSubSubRecord(TestContext context) {
    CheckController.checkMapperName = Country.class.getSimpleName();
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(Country.class);
    IMapper cityMapper = netRelay.getDatastore().getMapperFactory().getMapper(City.class);
    IMapper streetMapper = netRelay.getDatastore().getMapperFactory().getMapper(Street.class);
    Country tmpCountry = initCountry(context);
    String street = "Karl-August-Strasse MODIFIED";
    City city = tmpCountry.cities.get(0);

    try {
      String entityDef = RecordContractor.generateEntityReference(mapper, tmpCountry);
      entityDef += ".cities" + RecordContractor.createIdReference(cityMapper, city);
      entityDef += ".streets" + RecordContractor.createIdReference(streetMapper, city.streets.get(0));
      String url = String.format(UPDATE_CITY_URL + "?action=UPDATE&entity=%s", entityDef);

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
    context.assertEquals(1, savedCountry.cities.get(0).streets.size(), "Expected one streets");

    boolean found = false;
    for (Street str : savedCountry.cities.get(0).streets) {
      if (str.name.equals(street)) {
        found = true;
      }
    }
    context.assertTrue(found, "streetname was not modified");
  }

  @Test
  public void testUpdate(TestContext context) {
    CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = true;
    mapper.name = "testmapper for update";
    ResultContainer rc = DatastoreBaseTest.saveRecord(context, mapper);
    Object id = rc.writeResult.iterator().next().getId();
    LOGGER.info("ID: " + id);

    try {
      String reference = createReferenceAsCapturePart(context, mapper);
      String url = String.format("/products/%s/UPDATE/update.html", reference);
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME)
            .appendString(".name=updatePerformed");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME).appendString(".age=20");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        context.assertNotNull(resp);
        String content = resp.content;
        LOGGER.info("RESPONSE: " + content);
        context.assertTrue(content.contains("updatePerformed"), "Update was not performed");
        context.assertTrue(content.contains("20"), "updated age was not saved");
        // child was not modified in request and should stay true
        context.assertTrue(content.contains("true"), "property child was modified, but should not");
      }, 200, "OK", null);

    } catch (Exception e) {
      context.fail(e);
    }

  }

  @Test
  public void testUpdateAsParameter(TestContext context) {
    CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = true;
    mapper.name = "testmapper for update";
    ResultContainer rc = DatastoreBaseTest.saveRecord(context, mapper);
    String id = String.valueOf(rc.writeResult.iterator().next().getId());
    LOGGER.info("ID: " + id);

    try {
      String url = "/products/update2.html?"
          + createReferenceAsParameter(context, getPersistenceDef(), Action.UPDATE, mapper);
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME)
            .appendString(".name=updatePerformed");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME).appendString(".age=20");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        context.assertNotNull(resp);
        String content = resp.content;
        LOGGER.info("RESPONSE: " + content);
        context.assertTrue(content.contains("updatePerformed"), "Update was not performed");
        context.assertTrue(content.contains("20"), "updated age was not saved");
        // child was not modified in request and should stay true
        context.assertTrue(content.contains("true"), "property child was modified, but should not");
      }, 200, "OK", null);

      SimpleNetRelayMapper rec = (SimpleNetRelayMapper) DatastoreBaseTest.findRecordByID(context,
          SimpleNetRelayMapper.class, id);
      context.assertTrue(rec.child, "property child should not be changed");
    } catch (Exception e) {
      context.fail(e);
    }

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
    persistenceDefinition.setRoutes(new String[] { "/products/:entity/:action/update.html", "/products/update2.html",
        UPDATE_CUSTOMER_URL, UPDATE_CITY_URL });
    persistenceDefinition.getHandlerProperties().put(PersistenceController.UPLOAD_DIRECTORY_PROP,
        "webroot/images/productImages");
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), persistenceDefinition);
    setPersistenceDef(persistenceDefinition);

    RouterDefinition rd = new RouterDefinition();
    rd.setController(CheckController.class);
    CheckController.checkMapperName = SimpleMapper.class.getSimpleName();
    settings.getRouterDefinitions().addAfter(PersistenceController.class.getSimpleName(), rd);
  }

}
