/*
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.braintags.vertx.auth.datastore.IAuthenticatable;
import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;
import de.braintags.vertx.jomnigate.dataaccess.query.impl.IndexedField;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;

/**
 * PasswordLostClaim stores all information about a password lost process, performed by an {@link IAuthenticatable} (
 * Member etc ).
 * It is used as source to send the confirmation message and to give the ability to reset the password, when the user
 * clicked the link, which was contained inside the message
 * 
 * @author Michael Remme
 * 
 */
public class PasswordLostClaim extends AbstractRecord {
  private static final String DOT_REPLACEMENT = "§_§";

  public static final IIndexedField EMAIL = new IndexedField("email");
  public static final IIndexedField ACTIVE = new IndexedField("active");

  private String email;
  private boolean active = true;
  private Map<String, String> requestParameter = new HashMap<>();

  /**
   * 
   */
  public PasswordLostClaim() {
  }

  /**
   * @param email
   * @param password
   * @param request
   */
  public PasswordLostClaim(String email, HttpServerRequest request) {
    this.email = email;
    transfer(request.formAttributes(), requestParameter);
    transfer(request.params(), requestParameter);
  }

  private void transfer(MultiMap mm, Map<String, String> destination) {
    mm.entries().forEach(entry -> destination.put(entry.getKey(), entry.getValue()));
  }

  /**
   * The email address used to send the mail
   * 
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * @param email
   *          the email to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return the active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active
   *          the active to set
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * @return the requestParameter
   */
  @JsonSerialize(keyUsing = ReplaceDotKeySerializer.class)
  public Map<String, String> getRequestParameter() {
    return requestParameter;
  }

  /**
   * @param requestParameter
   *          the requestParameter to set
   */
  @JsonDeserialize(keyUsing = ReplaceDotKeyDeserializer.class)
  public void setRequestParameter(Map<String, String> requestParameter) {
    this.requestParameter = requestParameter;
  }

  public static class ReplaceDotKeyDeserializer extends KeyDeserializer {

    /*
     * (non-Javadoc)
     * 
     * @see com.fasterxml.jackson.databind.KeyDeserializer#deserializeKey(java.lang.String,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      return key.replaceAll(DOT_REPLACEMENT, ".");
    }

  }

  public static class ReplaceDotKeySerializer extends JsonSerializer<String> {

    /*
     * (non-Javadoc)
     * 
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException, JsonProcessingException {
      gen.writeFieldName(value.replaceAll("\\.", DOT_REPLACEMENT));
    }

  }

}
