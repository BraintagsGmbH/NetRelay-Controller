/**
 * === {@link de.braintags.netrelay.controller.api.MailController}
 * A controller which is sending mails by using the mail client, which is defined by NetRelay. The
 * controller can compose the content of the mail by using a static text, which will be set inside the configuration. Or
 * - if a template is defined by the configuration - the content will be created dynamic.
 * 
 * [source, json]
 * ----
 * {
 * "name" : "MailControllerCustomerContact",
 * "routes" : [ "/api/sendmailcustomercontact" ],
 * "controller" : "de.braintags.netrelay.controller.api.MailController",
 * "handlerProperties" : {
 * "templateDirectory" : "templates",
 * "template" : "mails/contactCustomer.html",
 * "mode" : "XHTML",
 * "from" : "service@xxx.com",
 * "bcc": "service@xxx.com"
 * }
 * }
 * ----
 * 
 * 
 * === {@link de.braintags.netrelay.controller.api.DataTablesController}
 * A controller, which generates the input for a jquery datatable. The mapper, which shall be used, is specified by a
 * request parameter with the name {@value de.braintags.netrelay.controller.api.DataTablesController#MAPPER_KEY}
 * 
 * [source, json]
 * ----
 * {
 * "name" : "DataTableController",
 * "routes" : [ "/api/datatables" ],
 * "controller" : "de.braintags.netrelay.controller.api.DataTablesController",
 * "handlerProperties" : {
 * "cacheEnabled" : "false"
 * }
 * }
 * ----
 * 
 */
package de.braintags.netrelay.controller.api;
