/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2016 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.persistence;

import java.util.Collection;

/**
 * Instance is generated by {@link RecordContractor} to deliver the information, which are needed to delete a subrecord
 * 
 * @author Michael Remme
 * 
 */
@SuppressWarnings("rawtypes")
public class DeleteParameter {
  private Collection parentCollection;
  private Object deleteObject;

  /**
   * The colection, where a child object shall be handled
   * 
   * @return the parentCollection
   */
  public Collection getParentCollection() {
    return parentCollection;
  }

  /**
   * The colection, where a child object shall be handled
   * 
   * @param parentCollection
   *          the parentCollection to set
   */
  public void setParentCollection(Collection parentCollection) {
    this.parentCollection = parentCollection;
  }

  /**
   * The object to be deleted
   * 
   * @return the deleteObject
   */
  public Object getDeleteObject() {
    return deleteObject;
  }

  /**
   * The object to be deleted
   * 
   * @param deleteObject
   *          the deleteObject to set
   */
  public void setDeleteObject(Object deleteObject) {
    this.deleteObject = deleteObject;
  }

}
