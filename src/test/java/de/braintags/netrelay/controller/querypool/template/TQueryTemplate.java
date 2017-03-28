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
package de.braintags.netrelay.controller.querypool.template;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.braintags.vertx.jomnigate.mongo.MongoDataStore;
import de.braintags.vertx.jomnigate.mysql.MySqlDataStore;
import io.vertx.core.json.JsonObject;

/**
 * Unit test for {@link QueryTemplate}<br>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */

public class TQueryTemplate {

  private static final String TEST_RESOURCE_PATH = "src/test/resources/de/braintags/netrelay/controller/querypool/querytemplate/queries/";

  /**
   * Test the parsing of a valid JSON of a dynamic query that has an 'and' part at the beginning
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test
  public void testDynamicQuery_validJson_andFirst() throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "ValidDynamicQuery_AndFirst.json");

    ObjectMapper om = new ObjectMapper();
    QueryTemplate queryTemplate = om.readValue(jsonFile, QueryTemplate.class);

    DynamicQuery dynamicQuery = queryTemplate.getDynamicQuery();
    Assert.assertTrue("First part should be 'and'", dynamicQuery.getRootQueryPart().isAnd());
  }

  /**
   * Test the parsing of a valid JSON of a dynamic query that has a 'condition' part at the beginning
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test
  public void testDynamicQuery_validJson_conditionFirst() throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "ValidDynamicQuery_ConditionFirst.json");

    ObjectMapper om = new ObjectMapper();
    QueryTemplate queryTemplate = om.readValue(jsonFile, QueryTemplate.class);

    DynamicQuery dynamicQuery = queryTemplate.getDynamicQuery();
    Assert.assertTrue("First part should be 'condition'", dynamicQuery.getRootQueryPart().isCondition());
  }

  /**
   * Test the parsing of a valid JSON of a dynamic query that has no 'query' part
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test
  public void testQuery_validJson_withoutQuery() throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "ValidTemplate_NoQuery.json");

    ObjectMapper om = new ObjectMapper();
    QueryTemplate queryTemplate = om.readValue(jsonFile, QueryTemplate.class);

    Assert.assertNull("Dynamic query of the template should be null", queryTemplate.getDynamicQuery());
    Assert.assertNull("Native query of the template should be null", queryTemplate.getNativeQueries());
  }

  /**
   * Test the parsing of an invalid JSON of a dynamic query, with multiple 'condition' statements without surrounding
   * 'and' or 'or'
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test(expected = JsonMappingException.class)
  public void testDynamicQuery_invalidJson_multipleConditions()
      throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "InvalidDynamicQuery_MultipleConditions.json");

    ObjectMapper om = new ObjectMapper();
    om.readValue(jsonFile, QueryTemplate.class);
  }

  /**
   * Test the parsing of an invalid JSON of a dynamic query, with multiple query parts inside one JSON object
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test
  public void testDynamicQuery_invalidJson_multipleParts()
      throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "InvalidDynamicQuery_MultipleParts.json");
    ObjectMapper om = new ObjectMapper();
    try {
      om.readValue(jsonFile, QueryTemplate.class);
      Assert.fail("Expected JsonMappingException with InvalidSyntaxException as cause");
    } catch (JsonMappingException e) {
      Assert.assertTrue("Expected message contains 'Missing required creator property', but is " + e.getMessage(),
          e.getMessage().contains("Missing required creator property"));
    }
  }

  /**
   * Test the parsing of a valid JSON of a native query
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test
  public void testNativeQuery_validJson() throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "ValidNativeQuery.json");

    ObjectMapper om = new ObjectMapper();
    QueryTemplate queryTemplate = om.readValue(jsonFile, QueryTemplate.class);

    Assert.assertEquals("Expected 2 native queries", 2, queryTemplate.getNativeQueries().size());

    NativeQuery mongoQuery = queryTemplate.getNativeQueries().get(0);
    Assert.assertEquals(MongoDataStore.class, mongoQuery.getDatastore());
    // should not throw an exception
    new JsonObject(mongoQuery.getQuery());

    NativeQuery mysqlQuery = queryTemplate.getNativeQueries().get(1);
    Assert.assertEquals(MySqlDataStore.class, mysqlQuery.getDatastore());
    Assert.assertTrue(StringUtils.isNotBlank(mysqlQuery.getQuery()));
  }

  /**
   * Test the parsing of an invalid JSON of a native query with no datastore value
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test(expected = JsonMappingException.class)
  public void testNativeQuery_invalidJson_NoDatastore() throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "InvalidNativeQuery_NoDatastore.json");

    ObjectMapper om = new ObjectMapper();
    om.readValue(jsonFile, QueryTemplate.class);
  }

  /**
   * Test the parsing of an invalid JSON of a native query with an invalid datastore value
   *
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Test(expected = JsonMappingException.class)
  public void testNativeQuery_invalidJson_InvalidDatastore()
      throws JsonParseException, JsonMappingException, IOException {
    File jsonFile = new File(TEST_RESOURCE_PATH + "InvalidNativeQuery_InvalidDatastore.json");

    ObjectMapper om = new ObjectMapper();
    om.readValue(jsonFile, QueryTemplate.class);
  }

}
