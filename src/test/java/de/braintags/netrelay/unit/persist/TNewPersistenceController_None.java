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
package de.braintags.netrelay.unit.persist;

import org.junit.Test;

import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.persist.PersistenceControllerNew;
import de.braintags.netrelay.impl.NetRelayExt_FileBasedSettings;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.unit.AbstractPersistenceControllerTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TNewPersistenceController_None extends AbstractPersistenceControllerTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TNewPersistenceController_None.class);

  @Test
  public void testHandleAction_None(TestContext context) {

    try {
      String url = "/products/none.html?action=NONE&entity=" + NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
      testRequest(context, HttpMethod.POST, url, null, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        context.assertTrue(resp.content.toString().contains("action none"), "Expected name not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.NetRelayBaseTest#modifySettings(de.braintags.netrelay.init.Settings)
   */
  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    persistenceDefinition = PersistenceControllerNew.createDefaultRouterDefinition();
    persistenceDefinition.setRoutes(new String[] { "/products/:entity/:action/list.html",
        "/products/:entity/:action/detail.html", "/products/:entity/:action/list2.html", "/products/detail2.html" });
    persistenceDefinition.getHandlerProperties().put(PersistenceControllerNew.UPLOAD_DIRECTORY_PROP,
        "webroot/images/productImages");
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), persistenceDefinition);
  }

}
