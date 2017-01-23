package de.braintags.netrelay.controller.querypool.template;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.braintags.vertx.jomnigate.IDataStore;

/**
 * The native query part of {@link QueryTemplate}. Native queries only work for their configured datastore and can
 * contain functions that are only available for a specific implementation of {@link IDataStore}<br>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */
public class NativeQuery {
  private Class<? extends IDataStore> datastore;
  @JsonDeserialize(using = RawDeserializer.class)
  private String query;

  /**
   * Constructor using all fields. All parameters are required
   * 
   * @param datastore
   *          the specific {@link IDataStore} for this query
   * @param query
   *          the query, converted to string
   */
  @JsonCreator
  public NativeQuery(@JsonProperty(value = "datastore", required = true) Class<? extends IDataStore> datastore,
      @JsonProperty(value = "query", required = true) String query) {
    this.datastore = datastore;
    this.query = query;
  }

  /**
   * @return the specific {@link IDataStore} for this query
   */
  public Class<? extends IDataStore> getDatastore() {
    return datastore;
  }

  /**
   * @return the query, converted to string
   */
  public String getQuery() {
    return query;
  }

  /**
   * A deserializer that just returns the raw JSON as string. Some datastores may use JSON as their query syntax, which
   * must be passed to them without manipulation
   */
  private static class RawDeserializer extends JsonDeserializer<String> {

    /*
     * (non-Javadoc)
     * 
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      TreeNode tree = p.getCodec().readTree(p);
      return tree.toString();
    }

  }

}