package de.braintags.netrelay.controller.persist;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.delete.IDelete;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Action is used to delete single records
 * 
 * @author Michael Remme
 * 
 */
public class DeleteAction extends AbstractAction {

  /**
   * @param persitenceController
   */
  public DeleteAction(PersistenceControllerNew persitenceController) {
    super(persitenceController);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  void handle(String entityName, RoutingContext context, CaptureMap captureMap, Handler<AsyncResult<Void>> handler) {
    IDataStore datastore = getPersistenceController().getNetRelay().getDatastore();
    IMapper mapper = getMapper(entityName);
    IDelete<?> delete = datastore.createDelete(mapper.getMapperClass());
    IQuery query = getPersistenceController().getNetRelay().getDatastore().createQuery(mapper.getMapperClass());
    RecordContractor.extractId(mapper, captureMap, query);
    delete.setQuery(query);
    delete.delete(result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

}
