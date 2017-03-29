/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller;

import java.util.Properties;

import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.templatemode.TemplateMode;

import de.braintags.netrelay.routing.RouterDefinition;
import de.braintags.netrelay.templateengine.thymeleaf.ThymeleafTemplateEngineImplBt;
import de.braintags.vertx.util.exception.InitException;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

/**
 * This controller is used to process templates based on the template engine Thymeleaf
 * <br>
 * <br>
 * Config-Parameter:<br/>
 * <UL>
 * <LI>{@value #TEMPLATE_MODE_PROPERTY}<br/>
 * <LI>{@value #TEMPLATE_DIRECTORY_PROPERTY}<br/>
 * <LI>{@value #CONTENT_TYPE_PROPERTY}<br/>
 * <LI>{@value #CACHE_ENABLED_PROPERTY}<br/>
 * <LI>{@value #DIALECTS_PROPERTY}
 * <LI>{@value #MULTIPATH_PROPERTY}
 * </UL>
 * <br>
 * Request-Parameter:<br/>
 * <br/>
 * Result-Parameter:<br/>
 * <br/>
 *
 *
 * Example configuration:<br/>
 *
 * <pre>
  {
      "name" : "ThymeleafTemplateController",
      "routes" : [ "/*" ],
      "controller" : "de.braintags.netrelay.controller.ThymeleafTemplateController",
      "handlerProperties" : {
        "templateDirectory" : "templates",
        "mode" : "XHTML",
        "contentType" : "text/html"
      }
    }
 * </pre>
 *
 *
 * @author Michael Remme
 */
public class ThymeleafTemplateController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ThymeleafTemplateController.class);

  /**
   * The property, by which the mode of Thymeleaf is defined. By default this is set to "XHTML"
   */
  public static final String TEMPLATE_MODE_PROPERTY = "mode";

  /**
   * The property, by which the directory is defined, where templates are residing
   */
  public static final String TEMPLATE_DIRECTORY_PROPERTY = "templateDirectory";

  /**
   * The default directory for templates
   */
  public static final String DEFAULT_TEMPLATE_DIRECTORY = TemplateHandler.DEFAULT_TEMPLATE_DIRECTORY;

  /**
   * The property, which defines the content type to be handled. PEr default this is text/html
   */
  public static final String CONTENT_TYPE_PROPERTY = "contentType";

  /**
   * The default content type to be managed
   */
  public static final String DEFAULT_CONTENT_TYPE = TemplateHandler.DEFAULT_CONTENT_TYPE;

  /**
   * The property, by which one can switch on / off the caching of templates. Switching off can be useful in development
   * systems to get changes as soon
   */
  public static final String CACHE_ENABLED_PROPERTY = "cacheEnabled";

  /**
   * By using this property, you are able to add dialects to extend thymeleaf. The value is a csv list, where each entry
   * is like "dialectPrefix:dialectClass" or just "dialectClass", if the default prefix shall be used
   */
  public static final String DIALECTS_PROPERTY = "dialects";

  /**
   * If this property is set to true, then templates are searched first in the path WITHIN the defined template
   * directory. If not found, templates are searched in the path WITHOUT the template directory.
   * WARNING: to avoid confusions and potential file collisions, you should use this option only exceptional.
   */
  public static final String MULTIPATH_PROPERTY = "multiPath";

  private TemplateHandler templateHandler;

  /*
   * (non-Javadoc)
   *
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    String path = context.request().path();
    addNetRelayToContext(context);
    LOGGER.debug("handling template for url " + context.normalisedPath() + " | " + path);
    if (path.endsWith("/")) {
      LOGGER.info("REROUTING TO: " + path);
      path += "index.html";
      context.reroute(path);
    } else {
      templateHandler.handle(context);
    }
  }

  @Override
  public void initProperties(Properties properties) {
    LOGGER.debug("init " + getName());
    ThymeleafTemplateEngine thEngine = createTemplateEngine(getNetRelay().getVertx(), properties);
    templateHandler = TemplateHandler.create(thEngine, getTemplateDirectory(properties), getContentType(properties));
  }

  /**
   * Creates a ThymeleafEngine by using the defined properties
   *
   * @param properties
   * @return
   */
  public static ThymeleafTemplateEngine createTemplateEngine(Vertx vertx, Properties properties) {

    boolean multiPath = Boolean.valueOf((String) properties.getOrDefault(MULTIPATH_PROPERTY, "false"));
    ThymeleafTemplateEngine thEngine = new ThymeleafTemplateEngineImplBt(vertx, multiPath,
        getTemplateDirectory(properties));
    String tms = properties.getProperty(TEMPLATE_MODE_PROPERTY, ThymeleafTemplateEngine.DEFAULT_TEMPLATE_MODE.name());
    TemplateMode tm = TemplateMode.valueOf(tms);
    thEngine.setMode(tm);
    setCachable(thEngine, properties);
    addDialects(thEngine, properties);
    return thEngine;
  }

  @SuppressWarnings("unchecked")
  private static void addDialects(ThymeleafTemplateEngine thEngine, Properties properties) {
    try {
      String dP = (String) properties.getOrDefault(DIALECTS_PROPERTY, null);
      if (dP != null && dP.hashCode() != 0) {
        String[] dialects = dP.split(",");
        for (String dialect : dialects) {
          if (dialect.contains(":")) {
            String[] d = dialect.split(":");
            Class<? extends IDialect> dc = (Class<? extends IDialect>) Class.forName(d[1]);
            thEngine.getThymeleafTemplateEngine().addDialect(d[0], dc.newInstance());
          } else {
            Class<? extends IDialect> dc = (Class<? extends IDialect>) Class.forName(dialect);
            thEngine.getThymeleafTemplateEngine().addDialect(dc.newInstance());
          }
        }
      }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new InitException(e);
    }
  }

  /**
   * Get the info about the defined template directory in the properties
   *
   * @param props
   *          the configuration
   * @return the defined value inside the properties or {@value #DEFAULT_TEMPLATE_DIRECTORY}
   */
  public static String getTemplateDirectory(Properties props) {
    return (String) props.getOrDefault(TEMPLATE_DIRECTORY_PROPERTY, DEFAULT_TEMPLATE_DIRECTORY);
  }

  private String getContentType(Properties props) {
    return (String) props.getOrDefault(CONTENT_TYPE_PROPERTY, DEFAULT_CONTENT_TYPE);
  }

  private static void setCachable(ThymeleafTemplateEngine thEngine, Properties properties) {
    LOGGER.warn("CACHING property currently unsupported since version 3 of Thymeleaf");
  }

  /**
   * Creates a default definition for the current instance
   *
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(ThymeleafTemplateController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(ThymeleafTemplateController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/*" });
    return def;
  }

  /**
   * Get the default properties for an implementation of TemplateController
   *
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(TEMPLATE_MODE_PROPERTY, ThymeleafTemplateEngine.DEFAULT_TEMPLATE_MODE);
    json.put(CONTENT_TYPE_PROPERTY, DEFAULT_CONTENT_TYPE);
    json.put(TEMPLATE_DIRECTORY_PROPERTY, DEFAULT_TEMPLATE_DIRECTORY);
    return json;
  }

}
