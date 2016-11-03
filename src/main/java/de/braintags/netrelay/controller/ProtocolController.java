package de.braintags.netrelay.controller;

import java.net.URI;
import java.util.Properties;

import de.braintags.netrelay.RequestUtil;
import io.vertx.ext.web.RoutingContext;

/**
 * The ProtocolController forces the use of a certain protocol, like https for instance, for the defined routes. If for
 * a fitting route the required protocol is not used, a redirect will be sent
 * <br/>
 * 
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <UL>
 * <LI>protocol - the required protocol, which shall be used for a fitting definition
 * <LI>port - if for the new protocol not the default port is used, then this port can be defined here
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
 * {
      "name" : "ProtocolController",
      "routes" : [ "/checkout/*", "/backend/*", "/myAccount/*" ],
      "blocking" : false,
      "failureDefinition" : false,
      "controller" : "de.braintags.netrelay.controller.ProtocolController",
      "handlerProperties" : {
        "protocol" : "https",
        "port" : "647"
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
public class ProtocolController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ProtocolController.class);

  /**
   * The name of the parameter, which specifies the required protocol
   */
  public static final String PROTOCOL_PROPNAME = "protocol";

  /**
   * The name of the parameter, which specifies the new port
   */
  public static final String PORT_PROPNAME = "port";

  private String protocol;
  private int port = -1;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    String path = context.request().absoluteURI();
    LOGGER.info(path);
    if (!path.startsWith(protocol) && path.contains("://")) {
      URI sUri = URI.create(path);
      String host = sUri.getHost();
      String protDef = port > 0 ? ":" + port : "";
      String uPath = sUri.getPath();
      String query = sUri.getQuery() != null ? "?" + sUri.getQuery() : "";
      String path2 = protocol + "://" + host + protDef + uPath + query;
      RequestUtil.sendRedirect(context, path2, false);
    } else {
      context.next();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    protocol = readProperty(PROTOCOL_PROPNAME, null, true);
    port = Integer.parseInt(readProperty(PORT_PROPNAME, "-1", false));
  }

}
