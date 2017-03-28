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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import io.vertx.core.Vertx;
import io.vertx.ext.web.impl.Utils;

/**
 * Regular template resolver, which is loading the given template from the vertx filesystem
 *
 *
 * @author Michael Remme
 *
 */
public class ResourceTemplateResolver extends StringTemplateResolver {
  protected final Vertx vertx;
  private Path          templatePath;

  public ResourceTemplateResolver(Vertx vertx, String templateDir) {
    super();
    setName("vertx-web/Thymeleaf3");
    this.vertx = vertx;
    if (templateDir != null && templateDir != "") {
      this.templatePath = Paths.get(templateDir);
    }

  }

  @Override
  protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
      String template, Map<String, Object> templateResolutionAttributes) {
    String str;
    if (ownerTemplate != null && templatePath != null) {
      str = Utils.readFileToString(vertx, templatePath.resolve(template).toString());
    } else {
      str = Utils.readFileToString(vertx, template);
    }

    return new StringTemplateResource(str);
  }
}