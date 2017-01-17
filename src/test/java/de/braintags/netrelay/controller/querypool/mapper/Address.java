/*
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

/**
 * Test mapper for {@link TQueryPoolController}<br>
 *
 * @author sschmitt
 *
 */
public class Address extends AbstractRecord {

  public String street;

}
