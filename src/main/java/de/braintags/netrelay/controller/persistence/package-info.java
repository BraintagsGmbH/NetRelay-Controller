/**
 * === {@link de.braintags.netrelay.controller.persistence.PersistenceController}
 * The PersistenceController is the instance, which translates the parameters and data of a request into a datastore
 * based action. A request like "http://localhost/article/detail?entity=article{ID:5}" will be interpreted by the
 * controller to fetch the article with the id 5 from the datastore and to store it inside the context, so that is can
 * be displayed by a template engine.
 * 
 * The PersistenceController covers the most frequent use cases of datastore based actions by an http form, so that the
 * number of particular Controllers can be reduced to specialized implementations. On the other hand the
 * PersistenceController shall not give the ability to create uncontrollable datastore actions just by configuration, to
 * force the creation of dedicated, well tested controllers and to avoid unrecognized performace bottlenecks
 * 
 * To understand the configuration, you should read the section "Capture Collection" inside the NetRelay documentation
 * 
 * For more infos about how to secure data access, see
 * {@link de.braintags.netrelay.controller.authentication.AuthenticationController}
 * 
 * *Referencing subobjects* +
 * Imagine two mapper "Person" and "Phone". The Phone has the phone number and an ID.
 * The mapper Person has an ID field and another field "List<Phone> phoneNumbers".
 * 
 * To add a new phone number to a Person, you will call the link: +
 * `insertCustomer.html?action=INSERT&entity=Person{ID:5}.phoneNumbers` +
 * If in the same request you want to send the new Phone number, you will create a form, where you will add a field with
 * the name: +
 * `Person.phoneNumbers.phoneNumber` +
 * Of course this expects, that "insertCustomer.html" is added as valid route for the PersistenceController.
 * 
 * To update an existing phone number, you will call the url: +
 * `insertCustomer.html?action=UPDATE&entity=Person{ID:5}.phoneNumbers{ID:1}` +
 * and again to add an input field with the above name to the corresonding http form.
 * 
 * To delete an existing phone number from a person, you will call: +
 * `insertCustomer.html?action=DELETE&entity=Person{ID:5}.phoneNumbers{ID:1}` +
 * 
 * 
 * 
 * 
 * *Example configuration* +
 * This example configuration defines the Persistence-Controller to be active under the url /article/detail and will
 * let run the above described actions. +
 * "http://localhost/article/detail?entity=article{ID:5}" will load the article for display, +
 * "http://localhost/article/detail?entity=article{ID:5}&action=DELETE" will delete this article from the datastore +
 * 
 * 
 * [source, json]
 * ----
 * {
 * "name" : "PersistenceController",
 * "routes" : [ "/article/detail" ],
 * "controller" : "de.braintags.netrelay.controller.persistence.PersistenceController",
 * "handlerProperties" : {
 * "reroute" : "false",
 * "cleanPath" : "true",
 * "uploadDirectory" : "webroot/upload/",
 * "uploadRelativePath" : "upload/"
 * },
 * "captureCollection" : [ {
 * "captureDefinitions" : [ {
 * "captureName" : "entity",
 * "controllerKey" : "entity",
 * "required" : false
 * }, {
 * "captureName" : "action",
 * "controllerKey" : "action",
 * "required" : false
 * } ]
 * } ]
 * }
 * 
 * ----
 * 
 * 
 * 
 * 
 * 
 */
package de.braintags.netrelay.controller.persistence;
