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
 * === {@link de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController}
 * This controller builds the api to support the web base filemanager from
 * https://github.com/Studio-42/elFinder
 * 
 * An example configuration of the component would look like this:
 * 
 * [source, json]
 * ----
 * {
 *   "name" : "ElFinderController",
 *   "controller" : "de.braintags.netrelay.controller.filemanager.elfinder.ElFinderController",
 *   "routes" : [ "/fileManager/api" ],
 *   "handlerProperties" : {
 *     "rootDirectories" : "ROOTVOLUME:webroot"
 *   }
 * }
 * ----
 * Note: the path of the defined root directory in the example above defines the name of the volume before the colon,
 * like it is displayed in the elfinder component.
 * 
 * A template for thymeleaf to implement the ElFinder component would look like this:
 * 
 * [source, html]
 * ----
 * 
 * <!DOCTYPE html SYSTEM "http://www.thymeleaf.org/dtd/xhtml1-strict-thymeleaf-4.dtd">
 * <html xmlns="http://www.w3.org/1999/xhtml"
 * xmlns:th="http://www.thymeleaf.org">
 * <head>
 * <title>elFinder filemanager</title>
 * <link rel="stylesheet" type="text/css" href=
 * "//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/smoothness/jquery-ui.css" />
 * <script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js"></script>
 * <script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js"></script>
 * 
 * <link rel="stylesheet" type="text/css" media="screen" href="/static/elFinder-2.1.15/css/elfinder.min.css">
 * <script type="text/javascript" src="/static/elFinder-2.1.15/js/elfinder.min.js"></script>
 * 
 * <!-- Mac OS X Finder style for jQuery UI smoothness theme (OPTIONAL) -->
 * <link rel="stylesheet" type="text/css" media="screen" href="/static/elFinder-2.1.15/css/theme.css">
 * <script type="text/javascript" src="/static/js/i18n/elfinder.de.js"></script>
 * 
 * </head>
 * 
 * <body>
 * <div class="jumbotron">
 * <div class="container">
 * <h3>FileManager elFinder</h3>
 * </div>
 * </div>
 * <div class="container">
 * 
 * <script type="text/javascript" charset="utf-8">
 * $().ready(function() {
 * var elf = $('#elfinder').elfinder({
 * // lang: 'ru', // language (OPTIONAL)
 * url : '/fileManager/api' // connector URL (REQUIRED)
 * }).elfinder('instance');
 * });
 * </script>
 * 
 * <!-- Element where elFinder will be created (REQUIRED) -->
 * <div id="elfinder"></div>
 * 
 * </div>
 * 
 * </body>
 * </html>
 * 
 * 
 * ----
 */
package de.braintags.netrelay.controller.filemanager.elfinder;
