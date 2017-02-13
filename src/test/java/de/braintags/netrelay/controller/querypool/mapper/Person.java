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
package de.braintags.netrelay.controller.querypool.mapper;

import de.braintags.netrelay.controller.querypool.TQueryPoolController;
import de.braintags.netrelay.model.AbstractRecord;
import de.braintags.vertx.jomnigate.annotation.Entity;
import de.braintags.vertx.jomnigate.annotation.field.Embedded;

/**
 * Test mapper for {@link TQueryPoolController}<br>
 * <br>
 * Copyright: Copyright (c) 20.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */

@Entity
public class Person extends AbstractRecord {

  public String firstname;
  public String lastname;
  public String city;
  public String zip;
  public int age;
  public double score;
  @Embedded
  public Address address;

}
