/*
 * #%L
 * netrelay
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

import de.braintags.netrelay.controller.authentication.RegisterController;
import de.braintags.vertx.auth.datastore.IAuthenticatable;
import io.vertx.core.http.HttpServerRequest;

/**
 * RegisterClaim stores all information about a registration, performed by an {@link IAuthenticatable} ( Member etc ).
 * It is used as source to send the double opt in message and to improve the registration, when the user clicked the
 * link sent by the double opt in process
 * 
 * @author Michael Remme
 * 
 */
public class RegisterClaim extends PasswordLostClaim {
  private String password;
  private String destinationUrl;

  public RegisterClaim() {
  }

  /**
   * Creates an instance, where all needed information of the request to register are stored
   * 
   * @param request
   */
  public RegisterClaim(String email, String password, HttpServerRequest request) {
    super(email, request);
    this.password = password;
    destinationUrl = request.getParam(RegisterController.REG_CONFIRM_SUCCESS_URL_PROP);
  }

  /**
   * The password is added into the new user account after successfull conformation
   * 
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * The password is added into the new user account after successfull conformation
   * 
   * @param password
   *          the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * If the destinationUrl is set, then a redirect is sent to the defined url after a successfull confirmation
   * 
   * @return the destinationUrl
   */
  public String getDestinationUrl() {
    return destinationUrl;
  }

  /**
   * If the destinationUrl is set, then a redirect is sent to the defined url after a successfull confirmation
   * 
   * @param destinationUrl
   *          the destinationUrl to set
   */
  public void setDestinationUrl(String destinationUrl) {
    this.destinationUrl = destinationUrl;
  }

}
