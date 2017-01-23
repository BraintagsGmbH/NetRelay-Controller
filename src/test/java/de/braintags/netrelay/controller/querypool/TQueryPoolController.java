package de.braintags.netrelay.controller.querypool;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.querypool.mapper.Address;
import de.braintags.netrelay.controller.querypool.mapper.Person;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.NetRelayBaseConnectorTest;
import de.braintags.vertx.jomnigate.testdatastore.DatastoreBaseTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Route;

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

  /**
   * Test a simple native query. The query should only return people under the age of 30
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testNativeQuery(TestContext context) throws Exception {
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

    String requestPath = "/queries/testNative.html";
    testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
      String response = resp.content;
      logger.debug(requestPath + ":\n" + response);
      context.assertTrue(response.contains("ID: " + rightPerson.id));
      context.assertFalse(response.contains("ID: " + wrongPerson.id));
    }, 200, "OK", null);
  }

  /**
   * Test a dynamic query that is sorted by the score of the person in ascending order.
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_orderBy(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person person_higherScore = new Person();
    person_higherScore.firstname = "max";
    person_higherScore.lastname = "mustermann1";
    person_higherScore.zip = "47877";
    person_higherScore.score = 2.5;
    DatastoreBaseTest.saveRecord(context, person_higherScore);

    Person person_lowerScore = new Person();
    person_lowerScore.firstname = "max";
    person_lowerScore.lastname = "mustermann2";
    person_lowerScore.city = "willich";
    person_lowerScore.score = 1.5;
    DatastoreBaseTest.saveRecord(context, person_lowerScore);

    String requestPath = "/queries/testDynamic_orderBy.html";
    testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
      String response = resp.content;
      logger.debug(requestPath + ":\n" + response);
      int lowerScorePos = response.indexOf("ID: " + person_lowerScore.id);
      int higherScorePos = response.indexOf("ID: " + person_higherScore.id, lowerScorePos + 1);
      context.assertTrue(lowerScorePos > 0, "Response must contain the ID of the lower score person");
      context.assertTrue(higherScorePos > 0, "Response must contain the ID of the higher score person");
      context.assertTrue(lowerScorePos < higherScorePos, "Lower score person must come before the higher score person");
    }, 200, "OK", null);
  }

  /**
   * Test a dynamic query that has a variable in its field value condition. The variable must be resolved against the
   * request parameter.
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_withRequestParameterVariable(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person person = new Person();
    person.firstname = "paramvalue";
    DatastoreBaseTest.saveRecord(context, person);

    String requestPath = "/queries/testDynamic_withRequestParameterVariable.html?param=" + person.firstname;
    testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
      String response = resp.content;
      logger.debug(requestPath + ":\n" + response);
      context.assertTrue(response.contains("ID: " + person.id));
    }, 200, "OK", null);
  }

  /**
   * Test a dynamic query that as a field value variable which must be filled from a mapper of the routing context. The
   * variable is multiple fields deep, which means the mapper contains a mapper which contains the value: "person ->
   * address -> street"
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_withDeepVariable(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);

    String value = "deep";

    Person person = new Person();
    person.address = new Address();
    person.address.street = value;
    person.firstname = value;
    DatastoreBaseTest.saveRecord(context, person);

    String requestPath = "/queries/testDynamic_withDeepVariable.html";
    Route route = null;
    try {
      // create a route to fill the routing context with the person before the query pool controller is called
      route = netRelay.getRouter().get(requestPath).order(1).handler(rt -> {
        rt.put("person", person);
        rt.next();
      });

      testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
        String response = resp.content;
        logger.debug(requestPath + ":\n" + response);
        context.assertTrue(response.contains("ID: " + person.id));
      }, 200, "OK", null);
    } finally {
      // ensure no following tests will be influenced
      if (route != null)
        route.remove();
    }
  }

  /**
   * Test a dynamic query with a variable that must be resolved by the data map of the routing context.
   * The request is made twice with a different value for the variable, to ensure that a once resolved variable field
   * value is not cached.
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_withContextVariable_executedTwice(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);

    String value = "con text";
    final AtomicInteger requestCount = new AtomicInteger(0);

    Person person = new Person();
    person.firstname = value;
    DatastoreBaseTest.saveRecord(context, person);

    String requestPath = "/queries/testDynamic_withContextVariable.html";
    Route route = null;
    try {
      // create a route to fill the routing context data with the test variable
      route = netRelay.getRouter().get(requestPath).order(1).handler(rt -> {
        if (requestCount.getAndIncrement() == 0)
          rt.put("key", value);
        else
          rt.put("key", "definitly not the value");
        rt.next();
      });

      Async async = context.async();
      testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
        String response = resp.content;
        logger.debug(requestPath + ":\n" + response);
        context.assertTrue(response.contains("ID: " + person.id));

        try {
          testRequest(context, HttpMethod.GET, requestPath, null, resp2 -> {
            String response2 = resp2.content;
            logger.debug(requestPath + ":\n" + response2);
            context.assertFalse(response2.contains("ID: " + person.id));
            async.complete();
          }, 200, "OK", null);
        } catch (Exception e) {
          context.fail();
        }

      }, 200, "OK", null);
    } finally {
      // ensure no following tests will be influenced
      if (route != null)
        route.remove();
    }
  }

  /**
   * Test a simple dynamic query with one field condition
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_simpleQuery(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person rightPerson = new Person();
    rightPerson.firstname = "max";
    DatastoreBaseTest.saveRecord(context, rightPerson);

    Person wrongPerson = new Person();
    wrongPerson.firstname = "maximilian";
    DatastoreBaseTest.saveRecord(context, wrongPerson);

    String requestPath = "/queries/testDynamic_simpleQuery.html";
    testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
      String response = resp.content;
      logger.debug(requestPath + ":\n" + response);
      context.assertTrue(response.contains("ID: " + rightPerson.id));
      context.assertFalse(response.contains("ID: " + wrongPerson.id));
    }, 200, "OK", null);
  }

  /**
   * Test the offset and limit function of the query, which should only return the second person since the offset and
   * limit are 1
   *
   * @param context
   * @throws Exception
   */
  @Test
  public void testDynamicQuery_limit(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person firstPerson = new Person();
    firstPerson.firstname = "max";
    firstPerson.score = 1.0;
    DatastoreBaseTest.saveRecord(context, firstPerson);

    Person secondPerson = new Person();
    secondPerson.firstname = "max";
    secondPerson.score = 2.0;
    DatastoreBaseTest.saveRecord(context, secondPerson);

    Person thirdPerson = new Person();
    thirdPerson.firstname = "max";
    thirdPerson.score = 3.0;
    DatastoreBaseTest.saveRecord(context, thirdPerson);

    String requestPath = "/queries/testDynamic_limit.html";
    testRequest(context, HttpMethod.GET, requestPath, null, resp -> {
      String response = resp.content;
      logger.debug(requestPath + ":\n" + response);
      context.assertFalse(response.contains("ID: " + firstPerson.id),
          "The first person should not have been found since the offset for the query is 1");
      context.assertTrue(response.contains("ID: " + secondPerson.id),
          "The second person should  have been found since the offset for the query is 1");
      context.assertFalse(response.contains("ID: " + firstPerson.id),
          "The third person should not have been found since the limit for the query is 1");
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
