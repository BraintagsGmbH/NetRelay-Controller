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

import com.fasterxml.jackson.databind.JsonNode;

import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
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
  private static final String VOLUME_ID = "ROOTVOLUME";
  private static final String ROOT_DIR = "/Users/mremme/workspace/vertx/NetRelay-Controller/tmp";

  /**
   * Comment for <code>API_ELFINDER</code>
   */
  private static final String API_ELFINDER = "/fileManager/api";

  // ?cmd=size&targets%5B0%5D=ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290L2ltYWdlcw_E_E&_=1475072637217

  @Test
  public void createFile(TestContext context) {
    String fn = "testfile.txt";
    if (vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
      vertx.fileSystem().deleteBlocking(ROOT_DIR + "/" + fn);
    }

    String hash = ElFinderContext.getHash(VOLUME_ID, ROOT_DIR);
    String url = API_ELFINDER + "?cmd=" + "mkfile&name=" + fn + "&target=" + hash + "&_=1475075436203";
    try {
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"));
        context.assertTrue(vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn), "file not created");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void sizeCommand(TestContext context) {
    String url = API_ELFINDER + "?cmd="
        + "size&targets%5B0%5D=ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290L2ltYWdlcw_E_E&_=1475072637217";

    try {
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"));
        JsonNode jo = Json.decodeValue(resp.content, JsonNode.class);
        context.assertTrue(resp.content.contains("size"), "reply must contain 'size'");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void openSubDir(TestContext context) {
    String url = API_ELFINDER
        + "?cmd=open&target=ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290L2ltYWdlcw_E_E&_=1475072637216";

    try {
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"));
        JsonNode jo = Json.decodeValue(resp.content, JsonNode.class);
        JsonNode files = jo.get("files");
        JsonArray array = new JsonArray(files.toString());

      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

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
        context.assertFalse(resp.content.contains("error"));

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
    def.getHandlerProperties().put(ElFinderController.ROOT_DIRECTORIES_PROPERTY, VOLUME_ID + ":" + ROOT_DIR);
    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), def);
    settings.getMappingDefinitions().addMapperDefinition("Member", Member.class);
  }

}
