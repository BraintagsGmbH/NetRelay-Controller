/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
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
import java.util.Objects;

import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.exception.FieldNotFoundException;
import de.braintags.netrelay.exception.ObjectRequiredException;
import de.braintags.netrelay.routing.CaptureCollection;
import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchConditionContainer;
import de.braintags.vertx.jomnigate.mapping.IProperty;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import de.braintags.vertx.jomnigate.mapping.IMapperFactory;
import de.braintags.vertx.util.assertion.Assert;

/**
 * Utility class which builds the contract between a request uri and a record
 *
 * @author Michael Remme
 *
 */
public class RecordContractor {
  /**
   * The character, which splits the name of the ID field from the value, like "ID:3"
   */
  private static final String ID_SPLIT = ":";
  /**
   * The character, which splits several ID definitions, like "ID:3, local:DE"
   */
  private static final String ID_SEPARATOR = ",";
  /**
   * The char, which opens the id specification inside the entity definition
   */
  public static final char OPEN_BRACKET = '(';
  /**
   * The char, which closes the id specification inside the entity definition
   */
  public static final char CLOSE_BRACKET = ')';

  /**
   *
   */
  private RecordContractor() {
  }

  /**
   * Method checks wether the entity definition references a subobject, like entity=Person(ID:4).phone
   *
   * @return true, if entity references a subobject
   */
  public static final boolean isSubobjectDefinition(CaptureMap map) {
    String mapperName = getEntityDefiniton(map);
    return mapperName.contains(".");
  }

  public static final DeleteParameter resolveDeleteParameter(IMapperFactory mapperFactory, Object mainObject,
      CaptureMap map) {
    String mapperName = getEntityDefiniton(map);
    int index = mapperName.indexOf('.');
    if (index < 0) {
      throw new IllegalArgumentException("the entity definition does not reference a subobject");
    }
    return resolveDeleteParameter(mapperFactory, mainObject, mapperName.substring(index + 1));
  }

  private static final DeleteParameter resolveDeleteParameter(IMapperFactory mapperFactory, Object parent,
      String entityDef) {
    int index = entityDef.indexOf('.');
    if (index < 0) {
      DeleteParameter dp = new DeleteParameter();
      IMapper mapper = mapperFactory.getMapper(parent.getClass());
      IProperty field = mapper.getField(extractEntityName(entityDef));
      dp.setParentCollection(readCollection(parent, field));
      dp.setDeleteObject(resolveNewParent(mapperFactory, parent, entityDef));
      return dp;
    } else {
      String objectReference = entityDef.substring(0, index);
      Object newParent = resolveNewParent(mapperFactory, parent, objectReference);
      return resolveDeleteParameter(mapperFactory, newParent, entityDef.substring(index + 1));
    }

  }

  /**
   * If an insert of a subobject shall be executed by en entity definition like Person(ID:4).phoneNumbers, then this
   * method
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
  public static final InsertParameter resolveUpdateParameter(IMapperFactory mapperFactory, Object mainObject,
      CaptureMap map) {
    String mapperName = getEntityDefiniton(map);
    int index = mapperName.indexOf('.');
    if (index < 0) {
      throw new IllegalArgumentException("the entity definition does not reference a subobject");
    }
    InsertParameter ip = resolveUpdateParameter(mapperFactory, mainObject, mapperName.substring(index + 1));
    ip.setFieldPath(extractEntityPath(map));
    return ip;
  }

  private static final InsertParameter resolveUpdateParameter(IMapperFactory mapperFactory, Object parent,
      String entityDef) {
    int index = entityDef.indexOf('.');
    if (index < 0) {
      InsertParameter ip = new InsertParameter();
      IMapper mapper = mapperFactory.getMapper(parent.getClass());
      IProperty field = mapper.getField(extractEntityName(entityDef));
      ip.setParentCollection(readCollection(parent, field));
      ip.setSubObjectMapper(mapperFactory.getMapper(field.getSubClass()));
      ip.setUpdateObject(resolveNewParent(mapperFactory, parent, entityDef));
      return ip;
    } else {
      String objectReference = entityDef.substring(0, index);
      Object newParent = resolveNewParent(mapperFactory, parent, objectReference);
      return resolveUpdateParameter(mapperFactory, newParent, entityDef.substring(index + 1));
    }
  }

  /**
   * If an insert of a subobject shall be executed by en entity definition like Person(ID:4).phoneNumbers, then this
   * method
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
    ip.setFieldPath(extractEntityPath(map));
    return ip;
  }

  private static final InsertParameter resolveInsertParameter(IMapperFactory mapperFactory, Object parent,
      String entityDef) {
    int index = entityDef.indexOf('.');
    if (index < 0) {
      InsertParameter ip = new InsertParameter();
      IMapper mapper = mapperFactory.getMapper(parent.getClass());
      IProperty field = mapper.getField(entityDef);
      ip.setParentCollection(readCollection(parent, field));
      ip.setSubObjectMapper(mapperFactory.getMapper(field.getSubClass()));
      return ip;
    } else {
      String objectReference = entityDef.substring(0, index);
      Object newParent = resolveNewParent(mapperFactory, parent, objectReference);
      return resolveInsertParameter(mapperFactory, newParent, entityDef.substring(index + 1));
    }
  }

  private static Object resolveNewParent(IMapperFactory mapperFactory, Object parent, String objectReference) {
    List<String[]> ids = extractIds(objectReference);
    String fieldName = extractEntityName(objectReference);
    IMapper mapper = mapperFactory.getMapper(parent.getClass());
    IProperty field = mapper.getField(fieldName);
    Collection<?> collection = readCollection(parent, field);
    if (collection == null || collection.isEmpty()) {
      throw new NullPointerException("Could not find expected collection for object reference " + objectReference);
    }
    IMapper subMapper = mapperFactory.getMapper(field.getSubClass());
    for (Object member : collection) {
      if (doesObjectFit(subMapper, ids, member)) {
        return member;
      }
    }
    throw new ObjectRequiredException("Could not find expected object for object reference " + objectReference);
  }

  private static boolean doesObjectFit(IMapper subMapper, List<String[]> ids, Object object) {
    for (String[] idDef : ids) {
      IProperty fieldDef = subMapper.getField(idDef[0]);
      if (fieldDef == null) {
        throw new FieldNotFoundException(subMapper, idDef[0]);
      }
      Object value = fieldDef.getPropertyAccessor().readData(object);
      if (value == null) {
        return false;
      } else if (!Objects.equals(String.valueOf(value), idDef[1])) {
        return false;
      }
    }
    return true;
  }

  private static Collection<?> readCollection(Object parent, IProperty field) {
    if (!field.isCollection()) {
      throw new UnsupportedOperationException(
          "autmatic filling of subobjects is working only with fields, which are of type Collection");
    }
    return (Collection<?>) field.getPropertyAccessor().readData(parent);
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
    int index = def.indexOf(OPEN_BRACKET);
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
    if (!ids.isEmpty()) {
      ISearchConditionContainer and = ISearchCondition.and();
      for (String[] id : ids) {
        IIndexedField indexedField;
        try {
          indexedField = IIndexedField.getIndexedField(id[0], mapper.getMapperClass());
        } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new FieldNotFoundException(mapper, id[0], e);
        }
        and.getConditions().add(ISearchCondition.isEqual(indexedField, id[1]));
      }
      query.setSearchCondition(and);
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
      IProperty field = mapper.getField(id[0]);
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
   *          the specification as String in the form of (ID:4)
   * @return
   */
  private static List<String[]> extractIds(String spec) {
    List<String[]> returnList = new ArrayList<>();
    int dotIndex = spec.indexOf('.');
    if (dotIndex > 0) {
      spec = spec.substring(0, dotIndex);
    }
    int index = spec.indexOf(OPEN_BRACKET);
    if (index > 0) {
      String specDef = spec.trim().substring(index + 1, spec.length() - 1);
      String[] defs = specDef.split(ID_SEPARATOR);
      for (String def : defs) {
        String[] keys = def.split(ID_SPLIT);
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
   *         "action=DISPLAY&entity=Person(ID:4)"
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
   * The reference will be like: entity=mapperName(ID:4)
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
   * Generates the reference sequence for a record ( and the subrecords ), something like "mapperName(ID:4)".
   *
   *
   * @param mapper
   *          the instance of {@link IMapper} which contains information of ID fields
   * @param record
   *          the instances to be referenced
   * @return a generated sequence
   */
  public static final String generateEntityReference(IMapper mapper, Object record) {
    return mapper.getMapperClass().getSimpleName() + createIdReference(mapper, record);
  }

  /**
   * Generates the pure ( encoded ) id sequence to reference a record, something like (ID:4)
   *
   * @param mapper
   * @param record
   * @return
   */
  public static final String createIdReference(IMapper mapper, Object record) {
    IProperty idField = mapper.getIdField().getField();
    Object id = idField.getPropertyAccessor().readData(record);
    Assert.notNull("id", id);
    return OPEN_BRACKET + idField.getName() + ID_SPLIT + id + CLOSE_BRACKET;
  }
}
