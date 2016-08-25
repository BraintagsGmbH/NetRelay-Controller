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
package de.braintags.netrelay.unit.persistence;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.testdatastore.DatastoreBaseTest;
import de.braintags.io.vertx.pojomapper.testdatastore.ResultContainer;
import de.braintags.io.vertx.pojomapper.testdatastore.mapper.SimpleMapper;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.impl.NetRelayExt_FileBasedSettings;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.mapper.SimpleNetRelayMapper;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractPersistenceControllerTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TPersistenceController_Update extends AbstractPersistenceControllerTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TPersistenceController_Update.class);

  @Test
  public void testUpdateSubRecord(TestContext context) {
    context.fail("unimplemented test");
  }

  @Test
  public void testUpdateSubSubRecord(TestContext context) {
    context.fail("unimplemented test");
  }

  @Test
  public void testUpdateNoIdField(TestContext context) {
    // Test neu: Aufruf mit Feld als Update, das keine ID ist
    context.fail("Test neu: Aufruf mit Feld als Update, das keine ID ist");
  }

  @Test
  public void testUpdate(TestContext context) {
    CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = true;
    mapper.name = "testmapper for update";
    ResultContainer rc = DatastoreBaseTest.saveRecord(context, mapper);
    Object id = rc.writeResult.iterator().next().getId();
    LOGGER.info("ID: " + id);

    try {
      String reference = createReferenceAsCapturePart(context, mapper);
      String url = String.format("/products/%s/UPDATE/update.html", reference);
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME)
            .appendString(".name=updatePerformed");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME).appendString(".age=20");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        context.assertNotNull(resp);
        String content = resp.content;
        LOGGER.info("RESPONSE: " + content);
        context.assertTrue(content.contains("updatePerformed"), "Update was not performed");
        context.assertTrue(content.contains("20"), "updated age was not saved");
        // child was not modified in request and should stay true
        context.assertTrue(content.contains("true"), "property child was modified, but should not");
      }, 200, "OK", null);

    } catch (Exception e) {
      context.fail(e);
    }

  }

  @Test
  public void testUpdateAsParameter(TestContext context) {
    CheckController.checkMapperName = NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME;
    SimpleNetRelayMapper mapper = new SimpleNetRelayMapper();
    mapper.age = 13;
    mapper.child = true;
    mapper.name = "testmapper for update";
    ResultContainer rc = DatastoreBaseTest.saveRecord(context, mapper);
    Object id = rc.writeResult.iterator().next().getId();
    LOGGER.info("ID: " + id);

    try {
      String url = "/products/update2.html?"
          + createReferenceAsParameter(context, persistenceDefinition, Action.UPDATE, mapper);
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME)
            .appendString(".name=updatePerformed");
        buffer.appendString("&").appendString(NetRelayExt_FileBasedSettings.SIMPLEMAPPER_NAME).appendString(".age=20");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        context.assertNotNull(resp);
        String content = resp.content;
        LOGGER.info("RESPONSE: " + content);
        context.assertTrue(content.contains("updatePerformed"), "Update was not performed");
        context.assertTrue(content.contains("20"), "updated age was not saved");
        // child was not modified in request and should stay true
        context.assertTrue(content.contains("true"), "property child was modified, but should not");
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
    persistenceDefinition = PersistenceController.createDefaultRouterDefinition();
    persistenceDefinition.setRoutes(new String[] { "/products/:entity/:action/update.html", "/products/update2.html" });
    persistenceDefinition.getHandlerProperties().put(PersistenceController.UPLOAD_DIRECTORY_PROP,
        "webroot/images/productImages");
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), persistenceDefinition);
    RouterDefinition rd = new RouterDefinition();
    rd.setController(CheckController.class);
    CheckController.checkMapperName = SimpleMapper.class.getSimpleName();
    settings.getRouterDefinitions().addAfter(PersistenceController.class.getSimpleName(), rd);
  }

}
