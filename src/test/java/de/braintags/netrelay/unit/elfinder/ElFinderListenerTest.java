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

import de.braintags.netrelay.controller.SessionController;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderConstants;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController;
import de.braintags.netrelay.controller.filemanager.elfinder.ICommandListener;
import de.braintags.netrelay.controller.filemanager.elfinder.command.ICommand;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.TargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import de.braintags.netrelay.util.MultipartUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;

/**
 *
 *
 * @author Michael Remme
 *
 */
public class ElFinderListenerTest extends AbstractCaptureParameterTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ElFinderListenerTest.class);
  private static final String VOLUME_ID = "ROOTVOLUME";
  private static final String ROOT_DIR = "tmp";
  private static final String ROOT_WEBROOT = "webroot";
  private IVolume vol;

  /**
   * Comment for <code>API_ELFINDER</code>
   */
  private static final String API_ELFINDER = "/fileManager/api";
  private static boolean LISTENED_BEFORE = false;
  private static boolean LISTENED_AFTER;

  @Test
  public void openFile(TestContext context) {
    LISTENED_BEFORE = false;
    LISTENED_AFTER = false;
    addListener();

    String fileContent = "content of a magic file";
    String fn = "file2Open.txt";
    if (!vertx.fileSystem().existsBlocking(ROOT_DIR + "/" + fn)) {
      vertx.fileSystem().writeFileBlocking(ROOT_DIR + "/" + fn, Buffer.buffer(fileContent));
    }

    String hash = ElFinderContext.getHash(getVolume().getRoot().createChildTarget(fn));
    String url = API_ELFINDER + "?cmd=" + "file&" + ElFinderConstants.ELFINDER_PARAMETER_TARGET + "=" + hash
        + "&_=1475075436203";
    try {
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

    context.assertTrue(LISTENED_BEFORE, "listener not called?");
    context.assertTrue(LISTENED_AFTER, "listener not called?");
  }

  private void addListener() {
    ICommandListener listener = new ICommandListener() {

      @Override
      public void before(ICommand command, ElFinderContext context, Handler<AsyncResult<Boolean>> handler) {
        LISTENED_BEFORE = true;
        Future f = Future.succeededFuture(true);
        handler.handle(f);
      }

      @Override
      public void after(ICommand command, ElFinderContext context, Object target, JsonObject resultObject,
          Handler<AsyncResult<Void>> handler) {
        LISTENED_AFTER = true;
        LOGGER.info("LISTENER CALLED: " + resultObject);
        Future f = Future.succeededFuture();
        handler.handle(f);
      }

    };

    RouterDefinition def = netRelay.getSettings().getRouterDefinitions()
        .getNamedDefinition(ElFinderController.class.getSimpleName());
    ElFinderController contr = (ElFinderController) def.getControllerInstance();
    contr.addCommandListener("file", listener);
  }

  IVolume getVolume() {
    if (vol == null) {
      vol = new VertxVolume(vertx.fileSystem(), ROOT_DIR, VOLUME_ID, null, new TargetSerializer());
    }
    return vol;
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
    settings.getRouterDefinitions().addAfter(SessionController.class.getSimpleName(), def);
    settings.getMappingDefinitions().addMapperDefinition("Member", Member.class);
  }

}
