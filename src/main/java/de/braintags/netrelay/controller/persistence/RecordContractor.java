package de.braintags.netrelay.controller.persistence;

import java.util.ArrayList;
import java.util.List;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.util.assertion.Assert;
import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.exception.FieldNotFoundException;
import de.braintags.netrelay.routing.CaptureCollection;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class RecordContractor {

  /**
   * 
   */
  private RecordContractor() {
  }

  /**
   * Extracts the entity name from the given {@link CaptureMap}
   * 
   * @param map
   *          the map to be used
   * @return the entity name
   */
  public static String extractEntityName(CaptureMap map) {
    String mapperName = map.get(PersistenceController.MAPPER_CAPTURE_KEY);
    if (mapperName == null) {
      throw new NullPointerException("no entity name specified");
    }
    int index = mapperName.indexOf('{');
    if (index > 0) {
      mapperName = mapperName.substring(0, index);
    }
    return mapperName;
  }

  /**
   * Method will extract the id parameter(s), improve the field names for the id specifications and will add the
   * suitable arguments to the query
   * 
   * @param mapper
   *          the mapper to be used
   * @param map
   *          the {@link CaptureMap}
   * @param query
   *          the query to be filled
   */
  public static void extractId(IMapper mapper, CaptureMap map, IQuery<?> query) {
    List<String[]> ids = extractIds(mapper, map);
    if (ids != null) {
      for (String[] id : ids) {
        query.field(id[0]).is(id[1]);
      }
    }
  }

  /**
   * Method will extract the id parameter(s) and improve the field names for the id specifications
   * 
   * @param mapper
   *          the mapper to be used
   * @param map
   *          the {@link CaptureMap}
   * @return a list whith key/value pairs, where the key is the fieldname
   */
  public static List<String[]> extractIds(IMapper mapper, CaptureMap map) {
    String mapperSpec = map.get(PersistenceController.MAPPER_CAPTURE_KEY);
    if (mapperSpec == null) {
      throw new NullPointerException("no entity name specified");
    }
    List<String[]> ids = extractIds(mapperSpec);
    if (ids != null) {
      for (String[] id : ids) {
        IField field = mapper.getField(id[0]);
        if (field == null) {
          throw new FieldNotFoundException(mapper, id[0]);
        }
      }
    }
    return ids;
  }

  /**
   * Extracts the id specification(s) as key-value pairs
   * 
   * @param spec
   *          the specification as String in the form of {id:5}
   * @return
   */
  private static List<String[]> extractIds(String spec) {
    int index = spec.indexOf('{');
    if (index > 0) {
      List<String[]> returnList = new ArrayList<>();
      String specDef = spec.trim().substring(index + 1, spec.length() - 1);
      String[] defs = specDef.split(",");
      for (String def : defs) {
        String[] keys = def.split(":");
        if (keys.length != 2) {
          throw new IllegalArgumentException(
              "Wrong id definition, expect one definition in the form of 'FieldName=fieldValue', not " + def);
        }
        returnList.add(keys);
      }
      return returnList;
    }
    return null;
  }

  /**
   * Creates the parameter sequence for a URL to reference a record
   * 
   * @param captureCollection
   *          the CaptureCollection from a {@link PersistenceController}
   * @param action
   *          the action to be executed
   * @param mapper
   *          the {@link IMapper} to be used
   * @param record
   *          the record to be referenced
   * @return the url parameter, which are referencing the record. This will be in the form
   *         "action=DISPLAY&entity=Person{ID=8}"
   */
  public static String generateReferenceParameter(CaptureCollection captureCollection, Action action, IMapper mapper,
      Object record) {
    String entityParameterName = captureCollection.getCaptureName(PersistenceController.MAPPER_CAPTURE_KEY);
    Assert.notNull("CaptureDefinition " + PersistenceController.MAPPER_CAPTURE_KEY, entityParameterName);
    String actionParameterName = captureCollection.getCaptureName(PersistenceController.ACTION_CAPTURE_KEY);
    Assert.notNull("CaptureDefinition " + PersistenceController.ACTION_CAPTURE_KEY, actionParameterName);
    return actionParameterName + "=" + action + "&" + generateReferenceParameter(mapper, entityParameterName, record);
  }

  /**
   * Generates the sequence, by which a record is referenced inside a URL.
   * The reference will be like: entity=mapperName{ID=8}
   * 
   * @param mapper
   *          the instance of {@link IMapper} which contains information of ID fields
   * @param entityParameterName
   *          the name of the parameter which will be used
   * @param record
   *          the instance to be referenced
   * @return a generated sequence
   */
  private static String generateReferenceParameter(IMapper mapper, String entityParameterName, Object record) {
    return entityParameterName + "=" + generateEntityReference(mapper, record);
  }

  /**
   * Generates the reference sequence for a record, something like "mapperName{ID=8}"
   * 
   * @param mapper
   *          the instance of {@link IMapper} which contains information of ID fields
   * @param record
   *          the instance to be referenced
   * @return a generated sequence
   */
  public static final String generateEntityReference(IMapper mapper, Object record) {
    return mapper.getMapperClass().getSimpleName() + createIdReference(mapper, record);
  }

  // {ID:8}
  private static final String createIdReference(IMapper mapper, Object record) {
    IField idField = mapper.getIdField();
    Object id = idField.getPropertyAccessor().readData(record);
    Assert.notNull("id", id);
    return RequestUtil.encodeText("{" + idField.getName() + ":" + id + "}");
  }

}
