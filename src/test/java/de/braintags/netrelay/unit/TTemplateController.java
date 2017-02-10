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

import de.braintags.netrelay.controller.ThymeleafTemplateController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * Test the TemplateController of NetRelay
 * 
 * @author Michael Remme
 * 
 */
public class TTemplateController extends NetRelayBaseConnectorTest {

  /**
   * Call a template with multipath, where the template exists not in template directory and as resource
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testIndexMultiPath_TemplateExistsInResource(TestContext context) throws Exception {
    resetRoutes(true);
    testRequest(context, HttpMethod.GET, "/de/braintags/resourceTemplate.html", 200, "OK");
  }

  /**
   * Call a template with multipath, where the template exists not in template directory and as resource
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testIndexMultiPath_TemplateExistsNOTInResource(TestContext context) throws Exception {
    resetRoutes(true);
    testRequest(context, HttpMethod.GET, "/de/braintags/resourceTemplateDoesNotExist.html", 500,
        "Internal Server Error");
  }

  /**
   * Call a template with multipath, where the template exists in the template directory
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testIndexMultiPath_TemplateExistsInDirectory(TestContext context) throws Exception {
    resetRoutes(true);
    testRequest(context, HttpMethod.GET, "/index.html", 200, "OK");
  }

  @Test
  public void testIndex(TestContext context) throws Exception {
    resetRoutes(false);
    testRequest(context, HttpMethod.GET, "/index.html", 200, "OK");
  }

  /**
   * Define a route for Templatehandler
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testDefinedRoute(TestContext context) throws Exception {
    resetRoutes(false, new String[] { "/testRoute/*" });
    testRequest(context, HttpMethod.GET, "/testRoute/route/routeIndex.html", 200, "OK");
    testRequest(context, HttpMethod.GET, "/index.html", 404, "Not Found");
  }

  /**
   * Define a route for Templatehandler
   * 
   * @param context
   * @throws Exception
   */
  @Test
  public void testDefinedRouteRegex(TestContext context) throws Exception {
    resetRoutes(false, new String[] { "regex:/route/.*" });
    testRequest(context, HttpMethod.GET, "/route/routeIndex.html", 200, "OK");
    testRequest(context, HttpMethod.GET, "/index.html", 404, "Not Found");
  }

  @Test
  public void testRedirect(TestContext context) throws Exception {
    resetRoutes(false);
    testRequest(context, HttpMethod.GET, "/", 200, "OK");
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.NetRelayBaseTest#initTest()
   */
  @Override
  public void initTest(TestContext context) {
    super.initTest(context);
  }

  private void resetRoutes(boolean multiPath) throws Exception {
    resetRoutes(multiPath, null);
    netRelay.resetRoutes();
  }

  private void resetRoutes(boolean multiPath, String[] routes) throws Exception {
    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(ThymeleafTemplateController.class.getSimpleName());
    def.getHandlerProperties().put(ThymeleafTemplateController.MULTIPATH_PROPERTY, String.valueOf(multiPath));
    def.setRoutes(routes);
    netRelay.resetRoutes();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.NetRelayBaseTest#modifySettings(de.braintags.netrelay.init.Settings)
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
  }

}
