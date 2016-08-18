package de.braintags.netrelay.controller.persist;

import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This action is just doing nothing and can be used to call a page, where a form inside, which shall not be processed,
 * for instance
 * 
 * @author Michael Remme
 * 
 */
public class NoneAction extends AbstractAction {

  /**
   * @param persitenceController
   */
  public NoneAction(PersistenceControllerNew persitenceController) {
    super(persitenceController);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.persistence.AbstractAction#handle(java.lang.String,
   * io.vertx.ext.web.RoutingContext, de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap,
   * io.vertx.core.Handler)
   */
  @Override
  void handle(String entityName, RoutingContext context, CaptureMap map, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

}
