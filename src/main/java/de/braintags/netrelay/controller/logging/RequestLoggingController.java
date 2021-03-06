/*-
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.logging;

import java.util.Properties;
import java.util.Set;

import de.braintags.netrelay.controller.AbstractController;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * This controller logs the request data into the logger and can be used for debugging purpose
 * 
 * Example configuration:
 * 
 * <pre>
     {
      "name" : "RequestLoggingController",
      "routes" : [ "/testtemplate/*", "/backend/*" ],
      "controller" : "de.braintags.netrelay.controller.logging.RequestLoggingController"
    }
 * 
 * </pre>
 * 
 * @author Michael Remme
 * 
 */
public class RequestLoggingController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(RequestLoggingController.class);

  /**
   * 
   */
  public RequestLoggingController() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handleController(RoutingContext context) {
    LOGGER.info("LOGGING REQUEST FOR " + context.request().path());

    MultiMap headers = context.request().headers();
    LOGGER.info("HEADERS: " + headers.size());
    headers.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    MultiMap params = context.request().params();
    LOGGER.info("PARAMETER: " + params.size());
    params.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    MultiMap formAttributes = context.request().formAttributes();
    LOGGER.info("FORM_ATTRIBUTES: " + formAttributes.size());
    formAttributes.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    Set<FileUpload> fileUploads = context.fileUploads();
    LOGGER.info("FILE UPLOADS: " + fileUploads.size());
    fileUploads.forEach(fu -> LOGGER.info("   NAME: " + fu.name() + " | FILENAME: " + fu.fileName() + " | UPLOADED: "
        + fu.uploadedFileName() + " | SIZE: " + fu.size()));

    LOGGER.info("USER: " + context.user());
    context.next();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
  }

}
