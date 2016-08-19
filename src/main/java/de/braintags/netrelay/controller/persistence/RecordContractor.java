/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mapping.IMapperFactory;
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
   * Method checks wether the entity definition references a subobject, like entity=Person{4}.phone
   * 
   * @return true, if entity references a subobject
   */
  public static final boolean isSubobjectDefinition(CaptureMap map) {
    String mapperName = getEntityDefiniton(map);
    return mapperName.contains(".");
  }

  /**
   * If an insert of a subobject shall be executed by en entity definition like Person{4}.phoneNumbers, then this method
   * extracts the needed objects and informations to execute the insert
   * 
   * @param datastore
   *          a datastore to be able to get mapper informations
   * @param mainObject
   *          the main object, which shall contain the sub object or the sub-sub object
   * @param map
   *          the CaptureMap, which contains the entity definition
   * @return an instance of InsertParameter with resolved information
   */
  public static final InsertParameter resolveInsertParameter(IMapperFactory mapperFactory, Object mainObject,
      CaptureMap map) {
    String mapperName = getEntityDefiniton(map);
    int index = mapperName.indexOf('.');
    if (index < 0) {
      throw new IllegalArgumentException("the entity definition does not reference a subobject");
    }
    InsertParameter ip = resolveInsertParameter(mapperFactory, mainObject, mapperName.substring(index + 1));
    ip.fieldPath = extractEntityPath(map);
    return ip;
  }

  private static final InsertParameter resolveInsertParameter(IMapperFactory mapperFactory, Object parent,
      String entityDef) {
    int index = entityDef.indexOf('.');
    if (index < 0) {
      InsertParameter ip = new InsertParameter();
      IMapper mapper = mapperFactory.getMapper(parent.getClass());
      IField field = mapper.getField(entityDef);
      if (!field.isCollection()) {
        throw new UnsupportedOperationException(
            "autmatic filling of subobjects is working only with fields, which are of type Collection");
      }
      ip.parentCollection = (Collection<?>) field.getPropertyAccessor().readData(parent);
      ip.subObjectMapper = mapperFactory.getMapper(field.getSubClass());
      return ip;
    } else {
      throw new UnsupportedOperationException("implement sub-sub object");
    }

  }

  /**
   * Extracts the pure path of an entity definition, with out some record references. The first entry is the mapper name
   * of the main object, the rest are fields and subfields
   * Something like Person.phoneNumbers or Country.cities.streets
   * 
   * @param map
   * @return
   */
  public static String extractEntityPath(CaptureMap map) {
    String returnString = null;
    String mapperName = getEntityDefiniton(map);
    String[] entries = mapperName.split("\\.");
    for (String entry : entries) {
      String tmp = extractEntityName(entry);
      returnString = returnString == null ? tmp : returnString + "." + tmp;
    }
    return returnString;
  }

  /**
   * Extracts the entity name from the given {@link CaptureMap}
   * 
   * @param map
   *          the map to be used
   * @return the entity name
   */
  public static String extractEntityName(CaptureMap map) {
    return extractEntityName(getEntityDefiniton(map));
  }

  /**
   * Extracts the pure entity name
   * 
   * @param map
   *          the map to be used
   * @return the entity name
   */
  private static String extractEntityName(String def) {
    int index = def.indexOf('{');
    if (index > 0) {
      def = def.substring(0, index);
    }
    return def;
  }

  /**
   * @param map
   * @return
   */
  private static String getEntityDefiniton(CaptureMap map) {
    String mapperName = map.get(PersistenceController.MAPPER_CAPTURE_KEY);
    if (mapperName == null) {
      throw new NullPointerException("no entity name specified");
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
    for (String[] id : ids) {
      query.field(id[0]).is(id[1]);
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
    String mapperSpec = getEntityDefiniton(map);
    List<String[]> ids = extractIds(mapperSpec);
    for (String[] id : ids) {
      IField field = mapper.getField(id[0]);
      if (field == null) {
        throw new FieldNotFoundException(mapper, id[0]);
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
    List<String[]> returnList = new ArrayList<>();
    int dotIndex = spec.indexOf('.');
    if (dotIndex > 0) {
      spec = spec.substring(0, dotIndex);
    }
    int index = spec.indexOf('{');
    if (index > 0) {
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
    }
    return returnList;
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

  @SuppressWarnings("rawtypes")
  public static class InsertParameter {
    private Collection parentCollection;
    private IMapper subObjectMapper;
    private String fieldPath = null;

    /**
     * @return the parentCollection
     */
    public Collection getParentCollection() {
      return parentCollection;
    }

    /**
     * @param parentCollection
     *          the parentCollection to set
     */
    public void setParentCollection(Collection parentCollection) {
      this.parentCollection = parentCollection;
    }

    /**
     * @return the subObjectMapper
     */
    public IMapper getSubObjectMapper() {
      return subObjectMapper;
    }

    /**
     * @param subObjectMapper
     *          the subObjectMapper to set
     */
    public void setSubObjectMapper(IMapper subObjectMapper) {
      this.subObjectMapper = subObjectMapper;
    }

    /**
     * Describes the pure path for a subobject, with out some record references. The first entry is the mapper name of
     * the main object, the rest are fields and subfields
     * Something like Person.phoneNumbers or Country.cities.streets
     * 
     * @return the fieldPath
     */
    public String getFieldPath() {
      return fieldPath;
    }

    public void addFieldPath(String newPath) {
      if (fieldPath == null) {
        fieldPath = newPath;
      } else {
        fieldPath += "." + newPath;
      }
    }
  }
}
