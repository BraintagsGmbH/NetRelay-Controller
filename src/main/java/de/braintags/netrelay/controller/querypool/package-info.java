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
 * = Query Pool
 * This package contains the query pool execution logic. The query pool is a pool of predefined queries that are more complex
 * than just an ID. To prevent the execution of complex queries on the fly, all of them must be created as a JSON file and put
 * in a predefined path. With this, there is always an overview which queries exist inside a system.
 * 
 * == Query Syntax
 * A query is build as a JSON object. The query must define:
 * - the mapper for which it will be executed
 * - a description of the general use of the query
 * - the operation that will be executed (SELECT, COUNT, ...)
 * - either a dynamic or a native query
 *  
 * Optionally, it can also define:
 * - the sort direction(s), a comma separated list of sort fields, optionally with ASC/DESC to indicate the direction
 * - a default limit for the number of results
 * - a default offset for the beginning of the results
 * 
 * === Native Query Syntax
 * A native query must have one or more entries with a specific datastore and a native query for that datastore.
 * On execution, the controller will look to what kind of datastore the defined mapper is assigned to, 
 * and execute the native query for this datastore.
 *  
 * Example:
 * [source, json]
 * ---- 
 * {
 *   "description": "This is a test query",
 *   "mapper": "Person",
 *   "operation": "select",
 *   "native": [
 *     {
 *       "datastore": "de.braintags.vertx.jomnigate.mongo.MongoDataStore",
 *       "query": {
 *         "firstname": "Max",
 *         "lastname": "Mustermann",
 *         "age": {
 *         	"$lt":30
 *         } 
 *       }
 *     },
 *     {
 *       "datastore": "de.braintags.vertx.jomnigate.mysql.MySqlDataStore",
 *       "query": "SELECT * FROM Person WHERE firstname = 'Max' AND lastname = 'Mustermann'"
 *     }
 *   ]
 * }
 * ----
 *
 * === Dynamic Query Syntax
 * A dynamic query will work with all datastores. 
 * The syntax of the query is similar to a simplified elasticsearch query structure.
 * The values of condition statements may also contain variables. Variables are strings that begin with "${" and end with "}". 
 * For the possible syntax and resolution, see {@link de.braintags.netrelay.controller.querypool.ContextFieldValueResolver}
 * 
 * Example:
 * [source, json]
 * ---- 
 * {
 *  "description": "This is a test query",
 *  "mapper": "Person",
 *  "operation": "select",
 *  "orderBy": "${request.params.order}",
 *  "dynamic": {
 *    "query": {
 *      "and": [
 *        {
 *          "condition": {
 *            "field": "firstname",
 *            "logic": "=",
 *            "value": "${request.name}"
 *          }
 *        },
 *        {
 *          "condition": {
 *            "field": "score",
 *            "logic": "=",
 *            "value": 2.5
 *          }
 *        },
 *        {
 *          "or": [
 *            {
 *              "condition": {
 *                "field": "city",
 *                "logic": "=",
 *                "value": "willich"
 *              }
 *            },
 *            {
 *              "condition": {
 *                "field": "zip",
 *                "logic": "=",
 *                "value": 47877
 *              }
 *            }
 *          ]
 *        }
 *      ]
 *    }
 *  }
 * }
 * ---- 
 * 
 * == {@link de.braintags.netrelay.controller.querypool.ContextFieldValueResolver}
 * This implementation of {@link de.braintags.vertx.jomnigate.dataaccess.query.IFieldValueResolver} converts a variable name to its value.
 * Currently, it works by checking the prefix of the variable name, and executing 3 different strategies depending on the prefix:
 * 1. context: Simply looks inside the data of the current request context for a matching key and value. Example: "${context:date}"
 * 2. request: Looks for a request parameter in the current request with the given name, and returns its value. Example: "${request:name}"
 * 3. mapper: Looks for one or more mappers in the current context with the given name. Example: "{mapper:person.address.city}"
 * If it's just one, the value of the given field name is returned. 
 * For more than one, a list of all the values of the given field for all entries is returned.
 * The resolution only happens directly before the execution and is not part of the cache.
 * 
 * 
 * == {@link de.braintags.netrelay.controller.querypool.template.QueryTemplate}
 * The query template, including the classes
 * - {@link de.braintags.netrelay.controller.querypool.template.DynamicQuery}
 * - {@link de.braintags.netrelay.controller.querypool.template.NativeQuery}
 * - {@link de.braintags.netrelay.controller.querypool.template.dynamic.Condition}
 * - {@link de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart}
 * is the java representation of the query JSON. 
 * These classes are configured to use the jackson JSON parser to allow a simple mapping from the JSON files to java classes, including some basic validation.
 *
 * == {@link de.braintags.netrelay.controller.querypool.QueryPoolController}
 * This controller reads predefined queries from the file system, and executes them on matching page calls. 
 * The queries must be stored as JSON objects in the defined format. 
 * On initialization, the JSON files are loaded and transformed into {@link de.braintags.vertx.jomnigate.dataaccess.query.IQuery} objects,
 * that are cached for the duration of the controller.
 * The path of the query file must match the path of the request for it to be executed.
 *
 * [source, json]
 * ----
 * {
 *   "name" : "QueryPoolController",
 *   "routes" : [   "*" ],
 *   "controller" : "de.braintags.netrelay.controller.querypool.QueryPoolController",
 *   "handlerProperties" : {
 *     "queryDirectory": "queries/"
 *   }
 * }
 * ----
 * 
 */
package de.braintags.netrelay.controller.querypool;
