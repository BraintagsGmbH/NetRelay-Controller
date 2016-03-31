/**
 * == Controllers
 * 
 * Description of the function of existing controllers. More detailed information about the configuration parameters,
 * return values etc. can be found inside the javadoc of the corresponding implementation.
 * 
 * === {@link de.braintags.netrelay.controller.RedirectController}
 * The RedirectController redirects fitting routes to a configurable destination
 * 
 * === {@link de.braintags.netrelay.controller.CurrentMemberController}
 * If a user is logged in, the propriate record is fetched from the datastore and stored in the context, so that it can
 * be used by following controllers, like a template controller, for instance
 * 
 * === {@link de.braintags.netrelay.controller.ThymeleafTemplateController}
 * This controller is used to process templates based on the template engine Thymeleaf
 * 
 * {@link de.braintags.netrelay.controller.persistence}
 * 
 * {@link de.braintags.netrelay.controller.authentication}
 * 
 * {@link de.braintags.netrelay.controller.api}
 * 
 * 
 * 
 */
package de.braintags.netrelay.controller;
