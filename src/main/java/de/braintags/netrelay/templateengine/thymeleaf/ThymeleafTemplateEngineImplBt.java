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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.LanguageHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

/**
 * A template engine for Thymeleaf, which allows different template resolver. If the property multipath is set to true,
 * then the {@link MultiPathResourceResolver} is used, otherwise the regular {@link ResourceTemplateResolver} is used
 *
 * @author Michael Remme
 *
 */
public class ThymeleafTemplateEngineImplBt implements ThymeleafTemplateEngine {

  private final TemplateEngine     templateEngine = new TemplateEngine();
  private ResourceTemplateResolver templateResolver;

  public ThymeleafTemplateEngineImplBt(Vertx vertx, boolean multiPath, String templateDirectory) {
    this.templateResolver = createResolver(vertx, multiPath, templateDirectory);
    this.templateEngine.setTemplateResolver(templateResolver);
  }

  private ResourceTemplateResolver createResolver(Vertx vertx, boolean multiPath, String templateDirectory) {
    ResourceTemplateResolver ts = multiPath ? new MultiPathResourceResolver(vertx, templateDirectory)
        : new ResourceTemplateResolver(vertx, templateDirectory);
    ts.setTemplateMode(ThymeleafTemplateEngine.DEFAULT_TEMPLATE_MODE);
    return ts;
  }

  @Override
  public ThymeleafTemplateEngine setMode(TemplateMode mode) {
    templateResolver.setTemplateMode(TemplateMode.HTML);
    return this;
  }

  @Override
  public TemplateEngine getThymeleafTemplateEngine() {
    return this.templateEngine;
  }

  @Override
  public void render(RoutingContext context, String templateFileName, Handler<AsyncResult<Buffer>> handler) {
    Buffer buffer = Buffer.buffer();

    Map<String, Object> data = new HashMap<>();
    data.put("context", context);
    data.putAll(context.data());

    synchronized (this) {

      final List<LanguageHeader> acceptableLocales = context.acceptableLanguages();

      LanguageHeader locale = null;

      if (!acceptableLocales.isEmpty()) {
        // this is the users preferred locale
        locale = acceptableLocales.get(0);
      }

      templateEngine.process(templateFileName, new WebIContext(data, locale), new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
          buffer.appendString(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {
          // not used
        }

        @Override
        public void close() throws IOException {
          // not used
        }
      });
      handler.handle(Future.succeededFuture(buffer));
    }

  }

  private static class WebIContext implements IContext {
    private final Map<String, Object> data;
    private final java.util.Locale    locale;

    private WebIContext(Map<String, Object> data, LanguageHeader locale) {
      this.data = data;
      this.locale = locale == null ? java.util.Locale.getDefault() : generate(locale);
    }

    private static java.util.Locale generate(LanguageHeader locale) {
      String variant;
      String lang = locale.tag();
      String country = locale.subtag();
      return lang != null && country != null
          ? new java.util.Locale(lang, country, (variant = locale.subtag(2)) == null ? "" : variant)
          : (lang != null ? new java.util.Locale(lang) : java.util.Locale.getDefault());
    }

    @Override
    public java.util.Locale getLocale() {
      return locale;
    }

    @Override
    public boolean containsVariable(String name) {
      return data.containsKey(name);
    }

    @Override
    public Set<String> getVariableNames() {
      return data.keySet();
    }

    @Override
    public Object getVariable(String name) {
      return data.get(name);
    }
  }

}
