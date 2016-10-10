package de.braintags.netrelay.unit.persistence;

import java.util.Properties;

import de.braintags.netrelay.controller.AbstractController;
import io.vertx.ext.web.RoutingContext;

public class CheckController extends AbstractController {
  static Throwable error;
  static String checkMapperName;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    Object o = context.get(checkMapperName);
    if (o == null) {
      error = new IllegalArgumentException("did not find an instance of " + checkMapperName + " in the context");
      context.fail(error);
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
  }

}