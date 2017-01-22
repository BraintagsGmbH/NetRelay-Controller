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
package de.braintags.netrelay.model;

import java.util.ArrayList;
import java.util.List;

import de.braintags.vertx.jomnigate.annotation.Entity;
import de.braintags.vertx.jomnigate.annotation.field.Embedded;

@Entity
public class Country extends AbstractRecord {
  public String name;

  @Embedded
  public List<City> cities = new ArrayList<City>();

}
