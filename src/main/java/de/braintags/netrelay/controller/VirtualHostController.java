package de.braintags.netrelay.controller;

import java.util.Properties;

import de.braintags.netrelay.RequestUtil;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.VirtualHostHandler;

/**
 * The VirtualHostController integrates the {@link VirtualHostHandler}. Like that it will verify, wether the hostname of
 * a request matches the defined hostName parameter. If so, it will send a redirect to the defined destination with the
 * defined http code.
 * <br/>
 * 
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <UL>
 * <LI>hostName - the name of the host, which will be checked. It allows wildcards to be used, like "*.myDomain.com"
 * <LI>destination - the destination url which us used, when a redirect is sent
 * <LI>appendPath - if true, then the path of the current request is appended to the new destination
 * <LI>code - the code to be used, when the redirect is sent
 * </UL>
 * 
 * Request-Parameter:<br/>
 * none
 * <br/>
 * 
 * Result-Parameter:<br/>
 * none
 * <br/>
 * 
 * Example configuration:<br/>
 * 
 * <pre>
  {
      "name" : "VirtualHostController",
      "routes" : null,
      "blocking" : false,
      "failureDefinition" : false,
      "controller" : "de.braintags.netrelay.controller.VirtualHostController",
      "httpMethod" : null,
      "handlerProperties" : {
        "hostName" : "127.0.0.1",
        "destination" : "http://localhost",
        "appendPath" : "true"
       },
      "captureCollection" : null
    }
 * </pre>
 * 
 * 
 * 
 * @author Michael Remme
 * 
 */
public class VirtualHostController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(VirtualHostController.class);

  /**
   * The name of the property which defines the host name
   */
  public static final String HOSTNAME_PROP = "hostName";

  /**
   * The name of the property which defines the code to be set
   */
  public static final String CODE_PROP = "code";

  /**
   * The name of the property which defines destination to be set
   */
  public static final String DESTINATION_PROP = "destination";

  /**
   * The name of the property which defines, wether the path shall be appended to the destination
   */
  public static final String APPEND_PATH_PROP = "appendPath";

  private VirtualHostHandler vHandler;
  private String destination;
  private boolean appendPath = true;
  private int code = 302;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    vHandler.handle(context);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    String hostName = readProperty(HOSTNAME_PROP, null, true);
    destination = readProperty(DESTINATION_PROP, null, true);
    appendPath = Boolean.parseBoolean(readProperty(APPEND_PATH_PROP, "true", false));
    code = Integer.parseInt(readProperty(CODE_PROP, "302", false));

    vHandler = VirtualHostHandler.create(hostName, context -> {
      String newDestination = destination;
      if (appendPath) {
        String path = context.request().path();
        path = path == null ? "" : path.startsWith("/") ? path : "/" + path;
        newDestination += path;
      }
      LOGGER.info("redirecting to " + newDestination);
      RequestUtil.sendRedirect(context, newDestination, true);
    });
  }

}
