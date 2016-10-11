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
package de.braintags.netrelay.templateengine.thymeleaf;

import java.util.HashMap;
import java.util.Map;

import de.braintags.io.vertx.util.exception.NoSuchFileException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.impl.Utils;
import io.vertx.ext.web.templ.TemplateEngine;

/**
 * An implementation of {@link TemplateHandler}.
 * This implementation has two possible modes:
 * if property "multiPath" is set to false, it is working like the default implementation from vertx and is searching
 * for templates inside the defined template directory only.
 * if the property "multipath" is set to true, then the TemplateHandler will first search for an existing template
 * inside the defined template directory. If none was found, the TemplateHandler will search for existing template in
 * the pure path, without the defined template directory, to allow the use of stored resources inside the classpath.
 * 
 * @author Michael Remme
 * 
 */
public class TemplateHandlerMultiPath implements TemplateHandler {
  private static final String NOT_FOUND = "multipath option under path '%s' and '%s'";
  private final TemplateEngine engine;
  private final String templateDirectory;
  private final String contentType;
  private boolean multiPath = false;
  /**
   * used if multiPath is set to true
   */
  private Map<String, String> pathLookup = new HashMap<>();

  public TemplateHandlerMultiPath(TemplateEngine engine, String templateDirectory, String contentType,
      boolean multiPath) {
    this.engine = engine;
    this.templateDirectory = templateDirectory;
    this.contentType = contentType;
    this.multiPath = multiPath;
  }

  @Override
  public void handle(RoutingContext context) {
    if (multiPath) {
      try {
        String file = resolveMultiPath(context);
        render(context, file);
      } catch (NoSuchFileException e) {
        context.fail(e);
      }
    } else {
      String file = templateDirectory + Utils.pathOffset(context.normalisedPath(), context);
      render(context, file);
    }
  }

  private String resolveMultiPath(RoutingContext context) throws NoSuchFileException {
    final String normalizedPath = context.normalisedPath();
    if (pathLookup.containsKey(normalizedPath)) {
      return pathLookup.get(normalizedPath);
    }
    String file = templateDirectory + Utils.pathOffset(normalizedPath, context);
    if (!context.vertx().fileSystem().existsBlocking(file)) {
      String cpFile = normalizedPath;
      if (cpFile.startsWith("/")) {
        cpFile = cpFile.substring(1);
      }
      if (context.vertx().fileSystem().existsBlocking(cpFile)) {
        file = cpFile;
      } else {
        throw new NoSuchFileException(String.format(NOT_FOUND, file, cpFile));
      }
    }
    pathLookup.put(normalizedPath, file);
    return file;
  }

  private void render(RoutingContext context, String file) {
    engine.render(context, file, res -> {
      if (res.succeeded()) {
        context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType).end(res.result());
      } else {
        context.fail(res.cause());
      }
    });
  }

}
