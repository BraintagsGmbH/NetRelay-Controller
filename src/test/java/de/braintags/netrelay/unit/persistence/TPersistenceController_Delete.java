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
package de.braintags.netrelay.unit.persistence;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.dataaccess.write.IWrite;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.util.ResultObject;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.controller.persistence.RecordContractor;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
import de.braintags.netrelay.model.City;
import de.braintags.netrelay.model.Country;
import de.braintags.netrelay.model.Street;
import de.braintags.netrelay.model.TestCustomer;
import de.braintags.netrelay.model.TestPhone;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractPersistenceControllerTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

/**
 *
 *
 * @author Michael Remme
 *
 */
public class TPersistenceController_Delete extends AbstractPersistenceControllerTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TPersistenceController_Delete.class);
  private static final String DELETE_CUSTOMER_URL = "/customer/deleteCustomer.html";
  private static final String DELETE_CITY_URL = "/country/deleteCity.html";

  @Test
  public void testDeleteSubSubRecord(TestContext context) {
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(Country.class);
    IMapper cityMapper = netRelay.getDatastore().getMapperFactory().getMapper(City.class);
    IMapper streetMapper = netRelay.getDatastore().getMapperFactory().getMapper(Street.class);
    Country tmpCountry = initCountry(context);
    City city = tmpCountry.cities.get(0);

    try {
      String entityDef = RecordContractor.generateEntityReference(mapper, tmpCountry);
      entityDef += ".cities" + RecordContractor.createIdReference(cityMapper, city);
      entityDef += ".streets" + RecordContractor.createIdReference(streetMapper, city.streets.get(0));
      String url = String.format(DELETE_CITY_URL + "?action=DELETE&entity=%s", entityDef);

      testRequest(context, HttpMethod.GET, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("Germany"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

    // after this request the customer must contain the phone-number
    Country savedCountry = (Country) DatastoreBaseTest.findRecordByID(context, Country.class, tmpCountry.id);
    context.assertNotNull(savedCountry, "could not find City in datastore");
    context.assertEquals(1, savedCountry.cities.size(), "Expected one city");
    context.assertNotNull(savedCountry.cities.get(0).streets, "streets are null");
    context.assertEquals(0, savedCountry.cities.get(0).streets.size(), "Expected zero city");

  }

  @Test
  public void testDeleteSubRecord(TestContext context) {
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(TestCustomer.class);
    IMapper phoneMapper = netRelay.getDatastore().getMapperFactory().getMapper(TestPhone.class);
    TestCustomer customer = initCustomer(context);
    String id = customer.getId();
    LOGGER.info("ID: " + id);

    try {
      // insert.html?entity=Person{ID:1}.phoneNumbers{ID:1}&action=DELETE
      String entityDef = RecordContractor.generateEntityReference(mapper, customer);
      entityDef += ".phoneNumbers" + RecordContractor.createIdReference(phoneMapper, customer.getPhoneNumbers().get(0));
      String url = String.format(DELETE_CUSTOMER_URL + "?action=DELETE&entity=%s", entityDef);

      testRequest(context, HttpMethod.GET, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("deleteSuccess"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

    // after this request the customer must contain the phone-number
    TestCustomer savedCustomer = (TestCustomer) DatastoreBaseTest.findRecordByID(context, TestCustomer.class, id);
    context.assertNotNull(savedCustomer, "could not find customer in datastore");
    context.assertEquals(0, savedCustomer.getPhoneNumbers().size(), "Expected zero phone numbers");
  }

  @Test
  public void testDeleteRecord(TestContext context) {
    Async async1 = context.async();
    IWrite<SimpleNetRelayMapper> write = netRelay.getDatastore().createWrite(SimpleNetRelayMapper.class);
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = false;
    mapper.name = "testmapper for display";
    write.add(mapper);
    ResultObject<SimpleNetRelayMapper> ro = new ResultObject<>(null);
    write.save(result -> {
      if (result.failed()) {
        context.fail(result.cause());
        async1.complete();
      } else {
        // all fine, ID should be set in mapper
        LOGGER.info("ID: " + mapper.id);
        async1.complete();
      }
    });
    async1.await();

    String id = mapper.id;
    Async async2 = context.async();
    try {
      String reference = createReferenceAsCapturePart(context, mapper);
      String url = String.format("/products/%s/DELETE/delete.html", reference);
      testRequest(context, HttpMethod.POST, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("deleteSuccess"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    } finally {
      async2.complete();
    }
    async2.await();
    IQuery<SimpleNetRelayMapper> query = netRelay.getDatastore().createQuery(SimpleNetRelayMapper.class);
    query.setSearchCondition(query.isEqual(query.getMapper().getIdField().getName(), id));
    DatastoreBaseTest.find(context, query, 0);
  }

  @Test
  public void testDeleteRecordAsParameter(TestContext context) {
    Async async1 = context.async();
    IWrite<SimpleNetRelayMapper> write = netRelay.getDatastore().createWrite(SimpleNetRelayMapper.class);
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = false;
    mapper.name = "testmapper for display";
    write.add(mapper);
    ResultObject<SimpleNetRelayMapper> ro = new ResultObject<>(null);
    write.save(result -> {
      if (result.failed()) {
        context.fail(result.cause());
        async1.complete();
      } else {
        // all fine, ID should be set in mapper
        LOGGER.info("ID: " + mapper.id);
        async1.complete();
      }
    });
    async1.await();

    Async async2 = context.async();
    String id = mapper.id;
    try {
      String url = "/products/delete2.html?"
          + createReferenceAsParameter(context, getPersistenceDef(), Action.DELETE, mapper);
      testRequest(context, HttpMethod.POST, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("deleteSuccess"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    } finally {
      async2.complete();
    }
    async2.await();
    IQuery<SimpleNetRelayMapper> query = netRelay.getDatastore().createQuery(SimpleNetRelayMapper.class);
    query.setSearchCondition(query.isEqual(query.getMapper().getIdField().getName(), id));
    DatastoreBaseTest.find(context, query, 0);
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
    persistenceDefinition.setRoutes(new String[] { "/products/:entity/:action/delete.html", "/products/delete2.html",
        DELETE_CUSTOMER_URL, DELETE_CITY_URL });
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), persistenceDefinition);
    setPersistenceDef(persistenceDefinition);
  }

}
