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
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.TargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.routing.RouterDefinitions;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  private static final String ROOT_DIR = "tmp";
  private static final String ROOT_WEBROOT = "webroot";
  private IVolume vol;

  /**
   * Comment for <code>API_ELFINDER</code>
   */
  private static final String API_ELFINDER = "/fileManager/api";

  // ?cmd=size&targets%5B0%5D=ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290L2ltYWdlcw_E_E&_=1475072637217

  @Test
  public void archiveCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void dimCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void duplicateCommand(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "file2Duplicate.txt";
      String fnDuplicated = "file2Duplicate(1).txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().createFileBlocking(ROOT_DIR + "/" + fn);
      }
      if (vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fnDuplicated)) {
        vertx.fileSystem().deleteBlocking(ROOT_DIR + "/" + fnDuplicated);
      }
      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "duplicate&" + ElFinderConstants.ELFINDER_PARAMETER_TARGETS + "=" + hash
          + "&_=1475075436203";
      testRequest(context, HttpMethod.GET, url, req -> {
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fnDuplicated), "file not duplicated");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void extractCommand(TestContext context) {
    // unimplemented command yet
  }

  @Test
  public void getCommand(TestContext context) {
    try {
      resetRoutes(null);
      // unimplemented as test yet
      String fileContent = "content of a magic file";
      String fn = "file2Open.txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().writeFileBlocking(ROOT_DIR + "/" + fn, Buffer.buffer(fileContent));
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "get&" + ElFinderConstants.ELFINDER_PARAMETER_TARGET + "=" + hash
          + "&_=1475075436203";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(resp.content.contains("content"), "content element not found");
        context.assertTrue(resp.content.contains(fileContent), "file content not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

  }

  @Test
  public void lsCommand(TestContext context) {
    // ?cmd=ls&target=ROOTVOLUME_xxx&intersect%5B%5D=expert+-+use-case-workshop+-+Braintags+GmbH.pdf&_=1475683250174

    // unimplemented as test yet
  }

  @Test
  public void parentsCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void pasteCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void putCommand(TestContext context) {
    try {
      resetRoutes(null);
      // target:
      // ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290L2luZGV4Lmh0bWw_E
      // content: testcontent um auch zu ändern to change

      String fileContent = "content of a magic file";
      String fn = "file2Open.txt";
      String filePath = ROOT_DIR + "/" + fn;
      if (vertx.fileSystem().existsBlocking(filePath)) {
        vertx.fileSystem().deleteBlocking(filePath);
      }

      vertx.fileSystem().writeFileBlocking(filePath, Buffer.buffer(fileContent));
      String editedContent = fileContent + " edited";
      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER;
      MultipartUtil mu = new MultipartUtil();
      mu.addFormField("cmd", "put");
      mu.addFormField(ElFinderConstants.ELFINDER_PARAMETER_TARGET, hash);
      mu.addFormField("content", editedContent);
      mu.addFormField("_", "1475075436203");
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        Buffer buffer = vertx.fileSystem().readFileBlocking(filePath);
        context.assertTrue(buffer.toString().contains(editedContent), "file content not written: " + buffer.toString());

      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }

  }

  @Test
  public void renameCommand(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "file2Rename.txt";
      String fnRenamed = "fileRenamed.txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().createFileBlocking(ROOT_DIR + "/" + fn);
      }
      if (vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fnRenamed)) {
        vertx.fileSystem().deleteBlocking(ROOT_DIR + "/" + fnRenamed);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "rename&name=" + fnRenamed + "&"
          + ElFinderConstants.ELFINDER_PARAMETER_TARGET + "=" + hash + "&_=1475075436203";
      testRequest(context, HttpMethod.GET, url, req -> {
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fnRenamed), "file not renamed");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void tmbCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void treeCommand(TestContext context) {
    // unimplemented as test yet
  }

  @Test
  public void uploadCommand(TestContext context) {
    // not implemented
  }

  @Test
  public void searchCommand(TestContext context) {
    try {
      resetRoutes(null);
      // http://localhost:8080/fileManager/api?cmd=search&q=some&target=ROOTVOLUME_L1VzZXJzL21yZW1tZS93b3Jrc3BhY2UvdmVydHgvTmV0UmVsYXktQ29udHJvbGxlci93ZWJyb290&_=1475659740124
      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(ROOT_WEBROOT));
      String url = API_ELFINDER + "?cmd=" + "search&q=some&" + ElFinderConstants.ELFINDER_PARAMETER_TARGET + "=" + hash
          + "&_=1475075436203";
      testRequest(context, HttpMethod.GET, url, req -> {
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(resp.content.contains("files"), "expected parameter 'files'");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void openFile(TestContext context) {
    try {
      resetRoutes(null);
      String fileContent = "content of a magic file";
      String fn = "file2Open.txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().writeFileBlocking(ROOT_DIR + "/" + fn, Buffer.buffer(fileContent));
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "file&" + ElFinderConstants.ELFINDER_PARAMETER_TARGET + "=" + hash
          + "&_=1475075436203";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(resp.content.contains(fileContent), "file content not found");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void deleteFile(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "file2Delete.txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().createFileBlocking(ROOT_DIR + "/" + fn);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "rm&" + ElFinderConstants.ELFINDER_PARAMETER_TARGETS + "=" + hash
          + "&_=1475075436203";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn), "file not deleted");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void deleteFullDirectory(TestContext context) {
    try {
      resetRoutes(null);
      String dir = "untertemp2DeleteFull";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + dir)) {
        vertx.fileSystem().mkdirBlocking(ROOT_DIR + "/" + dir);
      }
      String fn = dir + "/myFile.txt";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().createFileBlocking(ROOT_DIR + "/" + fn);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(dir));
      String url = API_ELFINDER + "?cmd=" + "rm&" + ElFinderConstants.ELFINDER_PARAMETER_TARGETS + "=" + hash
          + "&_=1475075436203";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertTrue(resp.content.contains("error"), "Error should have happend");
        context.assertTrue(resp.content.contains("Directory not empty"), "Error should have happend");
        context.assertTrue(vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + dir), "file was deleted");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void deleteDirectory(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "untertemp2Delete";
      if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().mkdirBlocking(ROOT_DIR + "/" + fn);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
      String url = API_ELFINDER + "?cmd=" + "rm&name=" + fn + "&" + ElFinderConstants.ELFINDER_PARAMETER_TARGETS + "="
          + hash + "&_=1475075436203";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"), "Error occured: " + resp.content);
        context.assertTrue(!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn), "file not deleted");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void createDirectory(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "untertemp";
      if (vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().deleteBlocking(ROOT_DIR + "/" + fn);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot());
      String url = API_ELFINDER + "?cmd=" + "mkdir&name=" + fn + "&target=" + hash + "&_=1475075436203";
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
  public void createFile(TestContext context) {
    try {
      resetRoutes(null);
      String fn = "testfile.txt";
      if (vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
        vertx.fileSystem().deleteBlocking(ROOT_DIR + "/" + fn);
      }

      String hash = ElFinderContext.getHash(getVolume().getRoot());
      String url = API_ELFINDER + "?cmd=" + "mkfile&name=" + fn + "&target=" + hash + "&_=1475075436203";
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
      resetRoutes(null);
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

    try {
      resetRoutes(null);
      String hash = ElFinderContext.getHash(getVolume().getRoot());
      String url = API_ELFINDER + "?cmd=open&target=" + hash + "&_=1475072637216";
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
    try {
      resetRoutes(null);
      String url = API_ELFINDER + "?cmd=open&target=&init=1&tree=1&_=1474899867097";
      MultipartUtil mu = new MultipartUtil();
      testRequest(context, HttpMethod.POST, url, req -> {
        mu.finish(req);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
        context.assertFalse(resp.content.contains("error"));
        JsonObject reply = new JsonObject(resp.content);
        context.assertTrue(reply.containsKey("api"), "api key not contained");
        context.assertTrue(reply.containsKey("files"), "files key not contained");

      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  IVolume getVolume() {
    if (vol == null) {
      vol = new VertxVolume(vertx.fileSystem(), ROOT_DIR, VOLUME_ID, null, new TargetSerializer());
    }
    return vol;
  }

  private void resetRoutes(String ignores) throws Exception {
    RouterDefinition def = defineRouterDefinition(ElFinderController.class, API_ELFINDER);
    def.getHandlerProperties().put(ElFinderController.ROOT_DIRECTORIES_PROPERTY, VOLUME_ID + ":" + ROOT_DIR);
    if (ignores == null) {
      def.getHandlerProperties().remove(ElFinderController.IGNORES);
    } else {
      def.getHandlerProperties().put(ElFinderController.IGNORES, ignores);
    }

    RouterDefinitions defs = netRelay.getSettings().getRouterDefinitions();
    defs.addOrReplace(def);
    netRelay.resetRoutes();
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
