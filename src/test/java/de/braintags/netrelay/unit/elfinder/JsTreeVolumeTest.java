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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.TargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import de.braintags.netrelay.controller.filemanager.jstree.JsTreeTargetSerializer;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class JsTreeVolumeTest extends AbstractCaptureParameterTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(JsTreeVolumeTest.class);

  public static final String ROOTDIR = "webroot";

  @Test
  public void testVolume(TestContext context) {
    Path path = FileSystems.getDefault().getPath(ROOTDIR);
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new TargetSerializer());
    context.assertTrue(vv.getRoot().exists());
    LOGGER.debug("DIRECTORY: " + vv.getRoot().toString());
    List<IVolume> rootVolumes = new ArrayList<>();
    rootVolumes.add(vv);
    ElFinderContext efContext = new ElFinderContext(null, rootVolumes);
    ITarget sub = vv.fromPath("images");
    String subPath = sub.getAbsolutePath();
    String rootPath = vv.getRoot().getAbsolutePath();

    context.assertTrue(sub.isChild(vv.getRoot()));
    context.assertEquals("images", sub.getName());
    context.assertEquals(vv.getRoot().getAbsolutePath() + "/images", sub.getPath().toString());

    String hashCode = sub.getHash();
    LOGGER.info("HASHCODE: " + hashCode);
    ITarget unHashed = efContext.fromHash(hashCode);
    context.assertEquals(sub.getPath(), unHashed.getPath());
    context.assertEquals(sub.getName(), unHashed.getName());

    context.assertTrue(vv.fromPath(vv.getRoot().getAbsolutePath()).isRoot(), "root node not detected");
    context.assertTrue(vv.fromPath(vv.getRoot().getPath()).isRoot(), "root node not detected");

  }

  @Test
  public void listChildren(TestContext context) {
    Path path = FileSystems.getDefault().getPath(ROOTDIR);
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new JsTreeTargetSerializer());
    context.assertTrue(vv.getRoot().exists());
    vv.getRoot().listChildren();

  }

}
