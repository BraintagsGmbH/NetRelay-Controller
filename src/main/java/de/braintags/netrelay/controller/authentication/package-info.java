
/**
 * === Authentication and registration
 * This package contains several controllers, which can be used to configure and use the complete process of
 * authentication, registration etc.
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.AuthenticationController}
 * All routes, which are covered by this controller are protected and require a valid login. The controller takes
 * automatically care about login and logout of users.
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.RegisterController}
 * This controller performs a user registration with an integrated automatic double opt in process
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.PasswordLostController}
 * The PasswordLostController is used to manage the process for a user, who doesn't remember his password. The process
 * integrates automatically double opt in.
 * 
 * 
 */
package de.braintags.netrelay.controller.authentication;