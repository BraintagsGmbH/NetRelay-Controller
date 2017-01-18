/*-
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

import org.junit.Test;

import de.braintags.netrelay.controller.ProtocolController;
import de.braintags.netrelay.controller.VirtualHostController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * Tests for {@link VirtualHostController}
 * 
 * @author Michael Remme
 * 
 */
public class TProtocolController extends NetRelayBaseConnectorTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TProtocolController.class);

  @Test
  public void testNoRedirectHttp(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    resetRoutes("http", -1);
    String url = "/index.html";
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
    }, 200, "OK", null);
  }

  @Test
  public void testRedirectWithoutPathPort(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    resetRoutes("https", 8888);
    String url = "/index.html";
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(location.equals("https://localhost:8888/index.html"), "redirect is incorrect: " + location);
    }, 302, "Found", null);
  }

  @Test
  public void testRedirectWithoutPathQuery(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    resetRoutes("https", -1);
    String url = "/index.html?ggg=1&jjjj=3";
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(location.equals("https://localhost/index.html?ggg=1&jjjj=3"),
          "redirect is incorrect: " + location);
    }, 302, "Found", null);
  }

  @Test
  public void testRedirectWithoutPathPortQuery(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    resetRoutes("https", 8888);
    String url = "/index.html?ggg=1&jjjj=3";
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(location.equals("https://localhost:8888/index.html?ggg=1&jjjj=3"),
          "redirect is incorrect: " + location);
    }, 302, "Found", null);
  }

  @Test
  public void testRedirectWithoutPath(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    resetRoutes("https", -1);
    String url = "/index.html";
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(location.equals("https://localhost/index.html"), "redirect is incorrect: " + location);
    }, 302, "Found", null);
  }

  private void resetRoutes(String protocol, int port) throws Exception {
    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(ProtocolController.class.getSimpleName());
    def.getHandlerProperties().put(ProtocolController.PROTOCOL_PROPNAME, protocol);
    def.getHandlerProperties().put(ProtocolController.PORT_PROPNAME, String.valueOf(port));
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
    RouterDefinition def = new RouterDefinition();
    def.setController(ProtocolController.class);
    def.getHandlerProperties().put(ProtocolController.PROTOCOL_PROPNAME, "https");
    settings.getRouterDefinitions().add(0, def);
  }

}
