package de.braintags.netrelay.controller.persist;

import java.util.List;
import java.util.Map;

import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.util.exception.ParameterRequiredException;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class UpdateAction extends InsertAction {

  /**
   * @param persitenceController
   */
  public UpdateAction(PersistenceControllerNew persitenceController) {
    super(persitenceController);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.persistence.InsertAction#extractProperties(java.lang.String,
   * io.vertx.ext.web.RoutingContext)
   */
  @Override
  protected Map<String, String> extractProperties(String entityName, CaptureMap captureMap, RoutingContext context,
      IMapper mapper) {
    Map<String, String> map = super.extractProperties(entityName, captureMap, context, mapper);
    List<String[]> ids = RecordContractor.extractIds(mapper, captureMap);
    boolean idFieldFound = false;
    for (String[] id : ids) {
      if (id[0].equalsIgnoreCase(mapper.getIdField().getName())) {
        idFieldFound = true;
        map.put(mapper.getIdField().getName().toLowerCase(), id[1]);
      }
    }
    if (!idFieldFound) {
      throw new ParameterRequiredException("The update action needs the id field in the record reference");
    }
    return map;
  }

}