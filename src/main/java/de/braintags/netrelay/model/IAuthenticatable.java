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

import java.util.List;

import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.authentication.RegisterController;

/**
 * The interface defines the base fields for an instance, who is able to register and to authenticate inside the current
 * system by using {@link AuthenticationController} and {@link RegisterController}
 * 
 * @author Michael Remme
 * 
 */
public interface IAuthenticatable {
  /**
   * The name of the property, which is used to store the current user in the context
   */
  public static final String CURRENT_USER_PROPERTY = "currentUser";

  /**
   * The email to be used for login and registration
   * 
   * @return
   */
  String getEmail();

  /**
   * The email to be used for login and registration
   * 
   * @param email
   */
  void setEmail(String email);

  /**
   * The password to be used for login and registration
   * 
   * @return
   */
  String getPassword();

  /**
   * The password to be used for login and registration
   * 
   * @param password
   */
  void setPassword(String password);

  /**
   * Defines the roles of a member, like admin, users etc. Roles are used by {@link AuthenticationController} for
   * instance to grant access to pages and other resources.
   * 
   * @return the roles
   */
  List<String> getRoles();

  /**
   * Defines the roles of a member, like admin, users etc. Roles are used by {@link AuthenticationController} for
   * instance to grant access to pages and other resources.
   * 
   * @param roles
   *          the roles to set
   */
  void setRoles(List<String> roles);

}
