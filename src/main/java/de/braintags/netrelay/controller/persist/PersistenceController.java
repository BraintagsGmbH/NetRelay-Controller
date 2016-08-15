package de.braintags.netrelay.controller.persist;

import java.util.List;
import java.util.Properties;

import de.braintags.netrelay.controller.AbstractCaptureController;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class PersistenceController extends AbstractCaptureController {

  /**
   * 
   */
  public PersistenceController() {
  }

  /* (non-Javadoc)
   * @see de.braintags.netrelay.controller.AbstractCaptureController#handle(io.vertx.ext.web.RoutingContext, java.util.List, io.vertx.core.Handler)
   */
  @Override
  protected void handle(RoutingContext context, List<CaptureMap> resolvedCaptureCollections,
      Handler<AsyncResult<Void>> handler) {
  }

  /* (non-Javadoc)
   * @see de.braintags.netrelay.controller.AbstractCaptureController#internalInitProperties(java.util.Properties)
   */
  @Override
  protected void internalInitProperties(Properties properties) {
  }

}
