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
package de.braintags.netrelay.controller.filemanager.elfinder;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import io.vertx.ext.web.RoutingContext;

/**
 * The context for all actions of ElFinder contains information about the root directory for instance
 * 
 * @author Michael Remme
 * 
 */
public class ElFinderContext {
  private static final String[][] ESCAPES = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" },
      { "=", "_E" } };

  private RoutingContext routingContext;
  private List<IVolume> rootVolumes;

  /**
   * 
   * @param context
   * @param rootVolumes
   */
  public ElFinderContext(RoutingContext context, List<IVolume> rootVolumes) {
    this.routingContext = context;
    this.rootVolumes = rootVolumes;
  }

  public RoutingContext getRoutingContext() {
    return routingContext;
  }

  public List<IVolume> getRootVolumes() {
    return rootVolumes;
  }

  /**
   * Resolves a given hash like defined by ElFinder into an {@link ITarget}
   * 
   * @param hash
   * @return
   */
  public ITarget fromHash(String hash) {
    for (IVolume v : rootVolumes) {
      String prefix = v.getId() + "_";

      if (hash.equals(prefix)) {
        return v.getRoot();
      }

      if (hash.startsWith(prefix)) {
        String localHash = hash.substring(prefix.length());

        for (String[] pair : ESCAPES) {
          localHash = localHash.replace(pair[1], pair[0]);
        }
        String relativePath = new String(Base64.getDecoder().decode(localHash));
        return v.fromPath(relativePath);
      }
    }

    return null;
  }

  public String translateHash(String hash) {
    for (IVolume v : rootVolumes) {
      String prefix = v.getId() + "_";

      if (hash.equals(prefix)) {
        return "/";
      }

      if (hash.startsWith(prefix)) {
        String localHash = hash.substring(prefix.length());

        for (String[] pair : ESCAPES) {
          localHash = localHash.replace(pair[1], pair[0]);
        }
        return new String(Base64.getDecoder().decode(localHash));
      }
    }
    return null;
  }

  /**
   * Creates a Hash like required by ElFinder from the given target
   * 
   * @param target
   * @return
   * @throws IOException
   */
  public static String getHash(ITarget target) {
    return getHash(target.getVolume().getId(), target.getPath());
  }

  /**
   * Creates a Hash like required by ElFinder from the given target
   * 
   * @param target
   * @return
   * @throws IOException
   */
  public static String getHash(String volumeId, String path) {
    String base = new String(Base64.getEncoder().encode(path.getBytes()));
    for (String[] pair : ESCAPES) {
      base = base.replace(pair[0], pair[1]);
    }
    return volumeId + "_" + base;
  }

  /**
   * Get the content for the given parameter of the current request
   * 
   * @param parameter
   * @return
   */
  public String getParameter(String parameter) {
    return routingContext.request().getParam(parameter);
  }

  /**
   * Get the content for the given parameter of the current request
   * 
   * @param parameter
   *          the name of the parameter
   * 
   * @return the List of parameters
   */
  public List<String> getParameterValues(String parameter) {
    return routingContext.request().params().getAll(parameter);
  }

}
