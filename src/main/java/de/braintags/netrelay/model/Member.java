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

import java.util.ArrayList;
import java.util.List;

import de.braintags.io.vertx.pojomapper.annotation.Entity;
import de.braintags.io.vertx.pojomapper.annotation.field.Id;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.vertx.auth.datastore.IAuthenticatable;

/**
 * Defines a member, which can be used inside a web application for authorization and authentication
 * 
 * @author Michael Remme
 * 
 */
@Entity
public class Member implements IAuthenticatable {
  @Id
  private String id;
  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private String gender;
  private List<String> roles = new ArrayList<>();
  private List<String> permissions = new ArrayList<>();

  /**
   * @return the userName
   */
  public final String getUserName() {
    return userName;
  }

  /**
   * @param userName
   *          the userName to set
   */
  public final void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * @return the firstName
   */
  public final String getFirstName() {
    return firstName;
  }

  /**
   * @param firstName
   *          the firstName to set
   */
  public final void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * @return the lastName
   */
  public final String getLastName() {
    return lastName;
  }

  /**
   * @param lastName
   *          the lastName to set
   */
  public final void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * @return the email
   */
  @Override
  public final String getEmail() {
    return email;
  }

  /**
   * @param email
   *          the email to set
   */
  @Override
  public final void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return the password
   */
  @Override
  public final String getPassword() {
    return password;
  }

  /**
   * @param password
   *          the password to set
   */
  @Override
  public final void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return the gender
   */
  public final String getGender() {
    return gender;
  }

  /**
   * @param gender
   *          the gender to set
   */
  public final void setGender(String gender) {
    this.gender = gender;
  }

  /**
   * @return the id
   */
  public final String getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public final void setId(String id) {
    this.id = id;
  }

  /**
   * Defines the roles of a member, like admin, users etc. Roles are used by {@link AuthenticationController} for
   * instance to grant access to pages and other resources.
   * 
   * @return the roles
   */
  @Override
  public List<String> getRoles() {
    return roles;
  }

  /**
   * Defines the roles of a member, like admin, users etc. Roles are used by {@link AuthenticationController} for
   * instance to grant access to pages and other resources.
   * 
   * @param roles
   *          the roles to set
   */
  @Override
  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  /**
   * @return the permissions
   */
  @Override
  public List<String> getPermissions() {
    return permissions;
  }

  /**
   * @param permissions
   *          the permissions to set
   */
  @Override
  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }

}
