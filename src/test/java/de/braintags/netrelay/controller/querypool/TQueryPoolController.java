package de.braintags.netrelay.controller.querypool;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.querypool.mapper.Person;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.NetRelayBaseConnectorTest;
import io.vertx.core.http.HttpMethod;
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

  /**
   * Base path for the resources used in query pool tests
   */
  private static final String TEST_RESOURCE_PATH = "src/test/resources/de/braintags/netrelay/controller/querypool/controller/";

  @Test
  public void testNativeQuery_validJson(TestContext context) throws Exception {
    DatastoreBaseTest.clearTable(context, Person.class);
    Person wrongPerson = new Person();
    wrongPerson.firstname = "Max";
    wrongPerson.lastname = "Mustermann";
    wrongPerson.age = 40;
    DatastoreBaseTest.saveRecord(context, wrongPerson);

    Person rightPerson = new Person();
    rightPerson.firstname = "Max";
    rightPerson.lastname = "Mustermann";
    rightPerson.age = 20;
    DatastoreBaseTest.saveRecord(context, rightPerson);

    testRequest(context, HttpMethod.GET, "/queries/testnative.html", null, resp -> {
      context.assertTrue(resp.content.contains("Firstname: " + rightPerson.firstname));
      context.assertTrue(resp.content.contains("Lastname: " + rightPerson.lastname));
      context.assertTrue(resp.content.contains("Age: " + rightPerson.age));
    }, 200, "OK", null);
  }

  /*
   * (non-Javadoc)
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
