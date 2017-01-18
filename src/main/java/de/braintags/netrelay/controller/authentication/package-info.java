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
 * === Authentication and registration
 * This package contains several controllers, which can be used to configure and use the complete process of
 * authentication, authorization ( permissions ), registration etc.
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.AuthenticationController}
 * This controller performs authentication ( login / logout ) and authorization ( permission handling, role access,
 * action access etc. ). All routes, which are covered by this controller are protected. The controller takes
 * automatically care about login and logout of users. Please read further documentation inside the javadoc of the
 * controller.
 * 
 * The configuration below protects all urls starting with /backend/system/ and /backend/dashboard/. Access is granted
 * for users with one of the roles marketing and admin, where marketing has the right to read and update records; admin
 * has the right to all actions
 * 
 * [source, json]
 * ----
 * {
 * "name" : "AuthenticationBackendController",
 * "routes" : [ "/backend/system/*", "/backend/dashboard/*" ],
 * "controller" : "de.braintags.netrelay.controller.authentication.AuthenticationController",
 * "handlerProperties" : {
 * "loginPage" : "/backend/login.html",
 * "logoutAction" : "/member/logout",
 * "roleField" : "roles",
 * "collectionName" : "Member",
 * "loginAction" : "/member/login",
 * "passwordField" : "password",
 * "usernameField" : "userName",
 * "authProvider" : "MongoAuth",
 * "permissions" : "role: marketing{RU}, admin{CRUD}"
 * }
 * }
 * ----
 * 
 * 
 * The configuration below protects the url /my-account/memberdata for users of any role. Users with the role "user" can
 * read and update records, users with the role "admin" can handle all actions on records and users with any other role
 * are only allowed to display records
 * 
 * [source, json]
 * ----
 * {
 * "name" : "AuthenticationMemberdataController",
 * "routes" : [ "/my-account/memberdata" ],
 * "controller" : "de.braintags.netrelay.controller.authentication.AuthenticationController",
 * "handlerProperties" : {
 * "loginPage" : "/backend/login.html",
 * "logoutAction" : "/member/logout",
 * "roleField" : "roles",
 * "collectionName" : "Member",
 * "loginAction" : "/member/login",
 * "passwordField" : "password",
 * "usernameField" : "userName",
 * "authProvider" : "MongoAuth",
 * "permissions" : "role: user{RU}, admin{CRUD}, *{R}"
 * }
 * }
 * 
 * ----
 * 
 * 
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.RegisterController}
 * This controller performs a user registration with an integrated automatic double opt in process.
 * To use this controller, you will have to create some templates:
 * 
 * * start of the registration process +
 * This template contains a form, which contains minimal two fields "email" and "password". Additional fields may be
 * defined by using the same structure than in the PersistenceController, like mapperName.fieldName ( for
 * example: "customer.lastName" ). The action of the form must point to a route, which is covered by the controller
 * definition ( here "/customer/doRegister" )
 * 
 * * register start success +
 * When the user sent the above form and the registration mail could be successfully sent, this template will be called.
 * 
 * * register start failed +
 * when the user sent the above form and the process could not be started ( cause the email exists already in the system
 * for instance ), then this template is called. The variable "registerError" contains an error variable.
 * 
 * * registration confirmation mail +
 * When the above form was sent, an email is sent to the customer, which contains a link, by which the validation is
 * processed. The link should be created like that ( Thymeleaf syntax): +
 * `<a th:href="${host}+'/my-account/verifyRegistration?validationId='+${context.get('validationId')}" target="_blank">
 * reset password</a>`
 * 
 * * registration confirmation success +
 * When the user clicks the link inside the confirmation mail, the controller tries to finish the process. If this is
 * successfull, this template will be called
 * 
 * * registration confirmation failed +
 * When the above process failed, this template will be called
 * 
 * 
 * [source, json]
 * ----
 * {
 * "name" : "RegisterCustomerController",
 * "routes" : [ "/customer/doRegister","/my-account/verifyRegistration"],
 * "controller" : "de.braintags.netrelay.controller.authentication.RegisterController",
 * "handlerProperties" : {
 * "regStartFailUrl" : "/my-account/registration.html",
 * "regStartSuccessUrl" : "/my-account/confirmRegistration.html",
 * "regConfirmSuccessUrl" : "/my-account/verifyRegistration.html",
 * "regConfirmFailUrl" : "/my-account/failureRegistration.html",
 * "templateDirectory" : "templates",
 * "template": "/mails/verifyEmail.html",
 * "mode" : "XHTML",
 * "from" : "service@xxx.com",
 * "bcc" : "service@xxx.com",
 * "subject": "Please verify your subscription",
 * }
 * }
 * 
 * 
 * ----
 * 
 * 
 * ==== {@link de.braintags.netrelay.controller.authentication.PasswordLostController}
 * The PasswordLostController is used to manage the process for a user, who doesn't remember his password. The process
 * integrates automatically double opt in.
 * To use this controller, you will have to create some templates:
 * 
 * * Activation of password lost +
 * This template contains a form, by which the email adress of the member or customer shall be sent. The address of the
 * form will be something like "/customer/passwordLost" - so one of the routes, which are covered by the controller.
 * 
 * * success url password lost
 * When the user sends the above form, the system tries to find his data and to send a mail with the link to finish the
 * process. If this was successful, then this template is called
 * 
 * * failed url password lost
 * if the above process failed for any reason, this template will be called, the property "resetError" contains some
 * information about the error
 * 
 * * Password lost mail +
 * The mail, which is sent to the customer, when his data are found, is generated by a template. This mail contains the
 * confirmation link, which will finish the password lost process. The link should be created like that ( Thymeleaf
 * syntax): +
 * `<a th:href="${host}+'/customer/passwordReset?validationId='+${context.get('validationId')}" target="_blank">reset
 * password</a>`
 * 
 * * Password reset success +
 * When the user clicks the link inside the confirmation mail, the controller tries to finish the process. If this is
 * successfull, this template will be called
 * 
 * * Password reset failed +
 * When the above process failed, this template will be called
 * 
 * 
 * [source, json]
 * ----
 * {
 * "name" : "PasswordLostController",
 * "routes" : [ "/customer/passwordLost","/customer/passwordReset"],
 * "controller" : "de.braintags.netrelay.controller.authentication.PasswordLostController",
 * "handlerProperties" : {
 * "pwLostFailUrl" : "/mein-konto/passwordLost.html",
 * "pwLostSuccessUrl" : "/mein-konto/confirmReset.html",
 * "pwResetSuccessUrl" : "/mein-konto/verifyReset.html",
 * "pwResetFailUrl" : "/mein-konto/failureReset.html",
 * "template": "/mails/passwordLostEmail.html",
 * "mode" : "XHTML",
 * "cacheEnabled" : "false",
 * "from" : "service@xxx.com",
 * "bcc" : "service@xxx.com",
 * "subject": "password lost"
 * }
 * }
 * 
 * 
 * ----
 * 
 */
package de.braintags.netrelay.controller.authentication;
