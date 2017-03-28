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

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import de.braintags.vertx.util.ExceptionUtil;
import de.braintags.vertx.util.exception.NoSuchFileException;
import io.vertx.core.Vertx;
import io.vertx.ext.web.impl.Utils;

/**
 * An implementation of {@link ITemplateResolver}
 * The resolver will first search for an existing template
 * inside the defined template directory. If none was found, the TemplateHandler will search for existing template in
 * the pure path, without the defined template directory, to allow the use of stored resources inside the classpath.
 *
 *
 * @author Michael Remme
 *
 */
public class MultiPathResourceResolver extends ResourceTemplateResolver {
  private static final String NOT_FOUND  = "multipath option under path '%s' and '%s'";
  private final String        templateDirectory;
  private Map<String, String> pathLookup = new HashMap<>();

  /**
   *
   */
  public MultiPathResourceResolver(Vertx vertx, String templateDirectory) {
    super(vertx, templateDirectory);
    this.templateDirectory = templateDirectory;
    setName("braintags/Thymeleaf3");
  }

  @Override
  protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
      String template, Map<String, Object> templateResolutionAttributes) {
    try {
      String str = Utils.readFileToString(vertx, resolveMultiPath(template));
      return new StringTemplateResource(str);
    } catch (NoSuchFileException e) {
      throw ExceptionUtil.createRuntimeException(e);
    }
  }

  private String resolveMultiPath(final String template) throws NoSuchFileException {
    if (pathLookup.containsKey(template)) {
      return pathLookup.get(template);
    }
    String file = template;
    if (!vertx.fileSystem().existsBlocking(file)) {
      String cpFile = templateDirectory + file;
        if (cpFile.startsWith("/")) {
        cpFile = cpFile.substring(1);
        }
        if (vertx.fileSystem().existsBlocking(cpFile)) {
          file = cpFile;
        } else {
          throw new NoSuchFileException(String.format(NOT_FOUND, file, cpFile));
        }

    }
    pathLookup.put(template, file);
    return file;
  }

}
