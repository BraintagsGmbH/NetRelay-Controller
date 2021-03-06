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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.TargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.impl.VertxVolume;
import de.braintags.netrelay.unit.AbstractCaptureParameterTest;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class ElFinderVolumeTest extends AbstractCaptureParameterTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ElFinderVolumeTest.class);

  public static final String ROOTDIR = "webroot";

  /**
   * check webroot and ignore directory "images"
   * 
   * @param context
   */
  @Test
  public void testVolumeIgnore(TestContext context) {
    List<String> ignores = Arrays.asList(".*/demofolder");
    Path path = FileSystems.getDefault().getPath(ROOTDIR);
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new TargetSerializer(), ignores);
    context.assertTrue(vv.getRoot().exists());
    LOGGER.debug("DIRECTORY: " + vv.getRoot().toString());
    List<IVolume> rootVolumes = new ArrayList<>();
    rootVolumes.add(vv);
    ElFinderContext efContext = new ElFinderContext(null, rootVolumes, ignores);

    List<ITarget> children = vv.getRoot().listChildren();
    int count = (int) children.stream().filter(target -> target.getName().equals("images")).count();
    context.assertEquals(1, count);

    // check manually the existence of the directory "demofolder"
    ITarget sub = vv.fromPath("demofolder");
    String subPath = sub.getAbsolutePath();
    context.assertTrue(sub.exists());
    // check that directory is suppressed in child output
    count = (int) children.stream().filter(target -> target.getName().equals("demofolder")).count();
    context.assertEquals(0, count);

  }

  @Test
  public void testVolume(TestContext context) {
    Path path = FileSystems.getDefault().getPath(ROOTDIR);
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new TargetSerializer(), null);
    context.assertTrue(vv.getRoot().exists());
    LOGGER.debug("DIRECTORY: " + vv.getRoot().toString());
    List<IVolume> rootVolumes = new ArrayList<>();
    rootVolumes.add(vv);
    ElFinderContext efContext = new ElFinderContext(null, rootVolumes, null);
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
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new TargetSerializer(), null);
    context.assertTrue(vv.getRoot().exists());
    vv.getRoot().listChildren();

  }

  @Test
  public void getRelativeChildPath(TestContext context) {
    Path path = FileSystems.getDefault().getPath(ROOTDIR);
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), path, "ROOTDIR", null, new TargetSerializer(), null);
    context.assertTrue(vv.getRoot().exists());
    List<ITarget> children = vv.getRoot().listChildren();
    context.assertFalse(children.isEmpty());
    ITarget child = children.get(0);
    String cp = child.getPath().toString();
    String vp = vv.getRoot().getPath().toAbsolutePath().toString();
    context.assertTrue(cp.startsWith(vp));

    String rp = child.getRelativePath();
    context.assertFalse(rp.startsWith(vp));
  }

}
