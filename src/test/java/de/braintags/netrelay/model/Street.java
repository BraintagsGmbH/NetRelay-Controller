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

import de.braintags.io.vertx.pojomapper.annotation.Entity;

@Entity
public class Street extends AbstractRecord {
  public String name;

  public Street() {

  }

  public Street(String name) {
    this.name = name;
  }

}
