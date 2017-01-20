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
/**
 * 
 * === {@link de.braintags.netrelay.controller.logging.RequestLoggingController}
 * This controller logs the request data into the logger and can be used for debugging purpose
 * 
 * [source, json]
 * ----
 * {
 * "name" : "RequestLoggingController",
 * "routes" : [ "/testtemplate/*", "/backend/*" ],
 * "controller" : "de.braintags.netrelay.controller.logging.RequestLoggingController"
 * }
 * ----
 */
package de.braintags.netrelay.controller.logging;
