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

import java.util.Map;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import io.vertx.core.Vertx;
import io.vertx.ext.web.impl.Utils;

public class ResourceTemplateResolver extends StringTemplateResolver {
  private Vertx vertx;

  public ResourceTemplateResolver() {
    super();
    setName("vertx-web/Thymeleaf3");
  }

  void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
      String template, Map<String, Object> templateResolutionAttributes) {
    String str = Utils.readFileToString(vertx, template);
    return new StringTemplateResource(str);
  }
}