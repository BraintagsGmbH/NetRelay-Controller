/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2016 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.unit.elfinder;

import org.junit.Test;

import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class ElFinderControllerTest extends AbstractCaptureParameterTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ElFinderControllerTest.class);

  /**
   * Comment for <code>API_ELFINDER</code>
   */
  private static final String API_ELFINDER = "/fileManager/api";

  @Test
  public void initialRequest(TestContext context) {
    String url = API_ELFINDER + "?cmd=open&target=&init=1&tree=1&_=1474899867097";

    try {
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);

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
    // defineRouterDefinitions adds the default key-definitions
    RouterDefinition def = defineRouterDefinition(ElFinderController.class, API_ELFINDER);
    def.getHandlerProperties().put(ElFinderController.ROOT_DIRECTORIES_PROPERTY,
        "ROOTVOL:/Users/mremme/workspace/vertx/NetRelay-Controller/webroot");
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), def);
    settings.getMappingDefinitions().addMapperDefinition("Member", Member.class);
  }

}
