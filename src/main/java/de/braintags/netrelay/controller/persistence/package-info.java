/**
 * === {@link de.braintags.netrelay.controller.persistence.PersistenceController}
 * The PersistenceController is the instance, which translates the parameters and data of a request into a datastore
 * based action. A request like "http://localhost/article/detail?ID=5&entity=article" will be interpreted by the
 * controller to fetch the article with the id 5 from the datastore and to store it inside the context, so that is can
 * be displayed by a template engine.
 * 
 */
package de.braintags.netrelay.controller.persistence;
