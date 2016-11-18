package de.braintags.netrelay.unit;

import org.junit.Test;

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
public class TVirtualHostController extends NetRelayBaseConnectorTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TVirtualHostController.class);

  @Test
  public void testNoRedirect(TestContext context) throws Exception {
    HOSTNAME = "localhost";
    String url = "/index.html";
    resetRoutes("127.0.0.1", "localhost", false, 302);
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
    }, 200, "OK", null);
  }

  @Test
  public void testRedirectWithoutPath(TestContext context) throws Exception {
    HOSTNAME = "127.0.0.1";
    String url = "/index.html";
    resetRoutes("127.0.0.1", "localhost:8080", false, 302);
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(!location.contains("/"), "redirect is incorrect, no path expected");
    }, 302, "Found", null);
  }

  @Test
  public void testRedirectWithPath(TestContext context) throws Exception {
    HOSTNAME = "127.0.0.1";
    String url = "/test.html";
    resetRoutes("127.0.0.1", "localhost:8080", true, 302);
    testRequest(context, HttpMethod.POST, url, req -> {
    }, resp -> {
      LOGGER.info("RESPONSE: " + resp.content);
      LOGGER.info("HEADERS: " + resp.headers);
      String location = resp.headers.get("location");
      context.assertNotNull(location, "no redirect sent");
      context.assertTrue(location.contains("/"), "redirect is incorrect, path expected");
    }, 302, "Found", null);
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

  private void resetRoutes(String host, String destination, boolean appendPath, int code) throws Exception {
    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(VirtualHostController.class.getSimpleName());
    def.getHandlerProperties().put(VirtualHostController.APPEND_PATH_PROP, String.valueOf(appendPath));
    def.getHandlerProperties().put(VirtualHostController.HOSTNAME_PROP, host);
    def.getHandlerProperties().put(VirtualHostController.CODE_PROP, String.valueOf(code));
    def.getHandlerProperties().put(VirtualHostController.DESTINATION_PROP, destination);
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
    def.setControllerClass(VirtualHostController.class);
    def.getHandlerProperties().put(VirtualHostController.HOSTNAME_PROP, "127.0.0.1");
    def.getHandlerProperties().put(VirtualHostController.DESTINATION_PROP, "localhost");
    settings.getRouterDefinitions().add(0, def);
  }

}
