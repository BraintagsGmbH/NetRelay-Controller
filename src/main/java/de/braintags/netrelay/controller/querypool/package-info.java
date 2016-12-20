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
 * === Native Query Syntax
 * A native query must have one or more entries with a specific datastore and a native query for that datastore.
 * On execution, the controller will look to what kind of datastore the defined mapper is assigned, 
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
 *       "datastore": "de.braintags.io.vertx.pojomapper.mongo.MongoDataStore",
 *       "query": {
 *         "firstname": "Max",
 *         "lastname": "Mustermann",
 *         "age": {
 *         	"$lt":30
 *         } 
 *       }
 *     },
 *     {
 *       "datastore": "de.braintags.io.vertx.pojomapper.mysql.MySqlDataStore",
 *       "query": "SELECT * FROM Person WHERE firstname = 'MAX' AND lastname = 'Mustermann'"
 *     }
 *   ]
 * }
 * ----
 *
 * === Dynamic Query Syntax
 * A dynamic query will work with all datastores. The syntax of the query is similar to a simplified elasticsearch query structure.
 * Beside the query itself, an "orderBy" clause may be defined, which, as the name implies, is the "order by" clause of the query.
 * Multiple fields can be seperated by comma, and the standard "ASC/DESC" direction after the field name.
 * 
 * Example:
 * [source, json]
 * ---- 
 * {
 *  "description": "This is a test query",
 *  "mapper": "Person",
 *  "operation": "select",
 *  "dynamic": {
 *    "orderBy": "${request.params.order}",
 *    "query": {
 *      "and": [
 *        {
 *          "condition": {
 *            "field": "firstname",
 *            "logic": "=",
 *            "value": "max"
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
 * == {@link de.braintags.netrelay.controller.querypool.template.QueryTemplate}
 * The query template, including the classes {@link de.braintags.netrelay.controller.querypool.template.DynamicQuery}, {@link de.braintags.netrelay.controller.querypool.template.NativeQuery}, {@link de.braintags.netrelay.controller.querypool.template.dynamic.Condition} and {@link de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart}, is the java representation of the query JSON. 
 * These classes are configured to use jackson to allow a simple mapping from the JSON files to java classes, including some basic validation.
 *
 * == {@link de.braintags.netrelay.controller.querypool.QueryPoolController}
 * This controller reads predefined queries from the file system, and executes them on matching page calls. 
 * The queries must be stored as JSON objects in the defined format. 
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