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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
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

  public static final String ROOTDIR = "/Users/mremme/workspace/vertx/NetRelay-Controller/webroot";

  @Test
  public void testVolume(TestContext context) {
    VertxVolume vv = new VertxVolume(vertx.fileSystem(), ROOTDIR, "ROOTDIR", null);
    List<IVolume> rootVolumes = new ArrayList<>();
    rootVolumes.add(vv);
    ElFinderContext efContext = new ElFinderContext(null, rootVolumes);
    ITarget sub = vv.fromPath(ROOTDIR + "/images");
    context.assertTrue(sub.getPath().startsWith(ROOTDIR));
    context.assertEquals("images", sub.getName());
    context.assertEquals(ROOTDIR + "/images", sub.getPath());

    String hashCode = sub.getHash();
    LOGGER.info("HASHCODE: " + hashCode);
    ITarget unHashed = efContext.fromHash(hashCode);
    context.assertEquals(sub.getPath(), unHashed.getPath());
    context.assertEquals(sub.getName(), unHashed.getName());

  }

}
