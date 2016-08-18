package de.braintags.netrelay.unit.persist;

import java.util.Properties;

import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
import io.vertx.ext.web.RoutingContext;

public class CheckController extends AbstractController {
  static Throwable error;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    Object o = context.get(SimpleNetRelayMapper.class.getSimpleName());
    if (o == null) {
      error = new IllegalArgumentException("did not find an instance of SimpleNetRelayMapper in the context");
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