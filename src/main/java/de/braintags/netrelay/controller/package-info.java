/**
 * == Controllers
 * 
 * Description of the function of existing controllers. More detailed information about the configuration parameters,
 * return values etc. can be found inside the javadoc of the corresponding implementation.
 * 
 * === {@link de.braintags.netrelay.controller.CurrentMemberController}
 * If a user is logged in, the corresponding record is fetched from the datastore and placed into the context with the
 * property {@value de.braintags.netrelay.model.IAuthenticatable#CURRENT_USER_PROPERTY}. From there the instance can be
 * accessed by other controllers like the {@link de.braintags.netrelay.controller.ThymeleafTemplateController}.
 * 
 * [source, json]
 * ----
 * {
 * "name" : "CurrentMemberController",
 * "routes" : null,
 * "blocking" : false,
 * "failureDefinition" : false,
 * "controller" : "de.braintags.netrelay.controller.CurrentMemberController",
 * "httpMethod" : null,
 * "handlerProperties" : { },
 * "captureCollection" : null
 * }
 * ----
 * 
 * === {@link de.braintags.netrelay.controller.ProtocolController}
 * The ProtocolController forces the use of a certain protocol, like https for instance, for the defined routes. If for
 * a fitting route the required protocol is not used, a redirect will be sent
 * 
 * [source, json]
 * ----
 * {
 * "name" : "ProtocolController",
 * "routes" : [ "/checkout/*", "/backend/*", "/myAccount/*" ],
 * "blocking" : false,
 * "failureDefinition" : false,
 * "controller" : "de.braintags.netrelay.controller.ProtocolController",
 * "handlerProperties" : {
 * "protocol" : "https",
 * "port" : "647"
 * },
 * }
 * ----
 * 
 * === {@link de.braintags.netrelay.controller.RedirectController}
 * The RedirectController redirects fitting routes to the page specified by property
 * {@value RedirectController#DESTINATION_PROPERTY}
 * 
 * 
 * [source, json]
 * ----
 * {
 * "name" : "RedirectController",
 * "routes" : [ "/" ],
 * "controller" : "de.braintags.netrelay.controller.RedirectController",
 * "handlerProperties" : {
 * "destination" : "/index.html"
 * },
 * }
 * ----
 * 
 * === {@link de.braintags.netrelay.controller.ThymeleafTemplateController}
 * This controller is used to process templates based on the template engine Thymeleaf
 * 
 * [source, json]
 * ----
 * {
 * "name" : "ThymeleafTemplateController",
 * "routes" : [ "/*" ],
 * "controller" : "de.braintags.netrelay.controller.ThymeleafTemplateController",
 * "handlerProperties" : {
 * "templateDirectory" : "templates",
 * "mode" : "XHTML",
 * "contentType" : "text/html",
 * "cacheEnabled" : "false"
 * }
 * }
 * ----
 * 
 * === {@link de.braintags.netrelay.controller.VirtualHostController}
 * The VirtualHostController integrates the VirtualHostHandler from vertx. Like that it will verify, wether the hostname
 * of a request matches the defined hostName parameter. If so, it will send a redirect to the defined destination with
 * the defined http code.
 * 
 * 
 * [source, json]
 * ----
 * {
 * "name" : "VirtualHostController",
 * "routes" : null,
 * "controller" : "de.braintags.netrelay.controller.VirtualHostController",
 * "handlerProperties" : {
 * "hostName" : "127.0.0.1",
 * "destination" : "http://localhost",
 * "appendPath" : "true"
 * },
 * "captureCollection" : null
 * }
 * 
 * ----
 * 
 * 
 * {@link de.braintags.netrelay.controller.authentication}
 * 
 * {@link de.braintags.netrelay.controller.persistence}
 * 
 * {@link de.braintags.netrelay.controller.logging}
 * 
 * {@link de.braintags.netrelay.controller.api}
 * 
 * 
 * 
 * [source, json]
 * ----
 * 
 * ----
 * 
 */
package de.braintags.netrelay.controller;
