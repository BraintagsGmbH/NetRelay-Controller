package de.braintags.netrelay.controller.querypool;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.querypool.mapper.Person;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.NetRelayBaseConnectorTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;

/**
 * Unit test for {@link QueryPoolController}<br>
 * <br>
 * Copyright: Copyright (c) 13.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */

public class TQueryPoolController extends NetRelayBaseConnectorTest {

  private static Logger logger = LoggerFactory.getLogger(TQueryPoolController.class);

  /**
   * Base path for the resources used in query pool tests
   */
  private static final String TEST_RESOURCE_PATH = "src/test/resources/de/braintags/netrelay/controller/querypool/controller/";

  @Test
  public void testNativeQuery_validJson(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person wrongPerson = new Person();
    wrongPerson.firstname = "max";
    wrongPerson.lastname = "mustermann";
    wrongPerson.age = 40;
    DatastoreBaseTest.saveRecord(context, wrongPerson);

    Person rightPerson = new Person();
    rightPerson.firstname = "max";
    rightPerson.lastname = "mustermann";
    rightPerson.age = 20;
    DatastoreBaseTest.saveRecord(context, rightPerson);

    testRequest(context, HttpMethod.GET, "/queries/testnative.html", null, resp -> {
      String response = resp.content;
      logger.debug(response);
      context.assertTrue(response.contains("ID: " + rightPerson.id));
      context.assertFalse(response.contains("ID: " + wrongPerson.id));
    }, 200, "OK", null);
  }

  @Test
  public void testDynamicQuery_validJson(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person wrongPerson = new Person();
    wrongPerson.firstname = "max";
    wrongPerson.lastname = "mustermann0";
    wrongPerson.score = 2.6;
    DatastoreBaseTest.saveRecord(context, wrongPerson);

    Person rightPerson_higherScore = new Person();
    rightPerson_higherScore.firstname = "max";
    rightPerson_higherScore.lastname = "mustermann1";
    rightPerson_higherScore.zip = "47877";
    rightPerson_higherScore.score = 2.5;
    DatastoreBaseTest.saveRecord(context, rightPerson_higherScore);

    Person rightPerson_lowerScore = new Person();
    rightPerson_lowerScore.firstname = "max";
    rightPerson_lowerScore.lastname = "mustermann2";
    rightPerson_lowerScore.city = "willich";
    rightPerson_lowerScore.score = 1.5;
    DatastoreBaseTest.saveRecord(context, rightPerson_lowerScore);

    testRequest(context, HttpMethod.GET, "/queries/testdynamic.html", null, resp -> {
      String response = resp.content;
      logger.debug(response);
      context.assertFalse(response.contains("ID: " + wrongPerson.id));
      int i = response.indexOf("ID: " + rightPerson_lowerScore.id);
      context.assertTrue(i > 0, "Response must contain the ID of the lower score person");
      i = response.indexOf("ID: " + rightPerson_higherScore.id, i + 1);
      context.assertTrue(i > 0,
          "Response must contain the ID of the higher score person, after the ID of the lower score person");
    }, 200, "OK", null);
  }

  @Test
  public void testDynamicQuery_validJson_withVariable(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person person = new Person();
    person.firstname = "paramvalue";
    DatastoreBaseTest.saveRecord(context, person);

    testRequest(context, HttpMethod.GET, "/queries/testdynamic_withvariable.html?param=" + person.firstname, null,
        resp -> {
          String response = resp.content;
          logger.debug(response);
          context.assertTrue(response.contains("ID: " + person.id));
        }, 200, "OK", null);
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

    RouterDefinition def = QueryPoolController.createDefaultRouterDefinition();
    def.setRoutes(new String[] { "/*" });
    def.getHandlerProperties().put(QueryPoolController.QUERY_DIRECTORY_PROPERTY, TEST_RESOURCE_PATH);
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), def);
    settings.getMappingDefinitions().addMapperDefinition(Person.class);
  }
}
