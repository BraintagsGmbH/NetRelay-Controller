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
package de.braintags.netrelay.unit;

import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.persistence.RecordContractor;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
import de.braintags.netrelay.model.Country;
import de.braintags.netrelay.model.TestCustomer;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractPersistenceControllerTest extends AbstractCaptureParameterTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AbstractPersistenceControllerTest.class);

  private static RouterDefinition persistenceDef;

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.NetRelayBaseTest#modifySettings(io.vertx.ext.unit.TestContext,
   * de.braintags.netrelay.init.Settings)
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    settings.getDatastoreSettings().setDatabaseName("TPersistenceController");
    settings.getMappingDefinitions().addMapperDefinition(TestCustomer.class);
    settings.getMappingDefinitions().addMapperDefinition(Country.class);
  }

  /**
   * @param context
   * @param record
   * @return
   */
  public static String createReferenceAsCapturePart(TestContext context, SimpleNetRelayMapper record) {
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(SimpleNetRelayMapper.class);
    String reference = RecordContractor.generateEntityReference(mapper, record);
    context.assertTrue(reference.contains("SimpleNetRelayMapper"), "mapper not referenced");
    return reference;
  }

  /**
   * @param context
   * @param record
   * @return
   */
  public static String createReferenceAsParameter(TestContext context, RouterDefinition persistenceDefinition,
      Action action, SimpleNetRelayMapper record) {
    IMapper mapper = netRelay.getDatastore().getMapperFactory().getMapper(record.getClass());
    String reference = RecordContractor.generateReferenceParameter(persistenceDefinition.getCaptureCollection()[0],
        action, mapper, record);
    context.assertTrue(reference.contains(record.getClass().getSimpleName()), "mapper not referenced");
    return reference;
  }

  /**
   * @return the persistenceDef
   */
  public RouterDefinition getPersistenceDef() {
    return persistenceDef;
  }

  /**
   * @param persistenceDef
   *          the persistenceDef to set
   */
  public void setPersistenceDef(RouterDefinition persistenceDef) {
    this.persistenceDef = persistenceDef;
  }

}
