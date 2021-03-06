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
package de.braintags.netrelay.model;

import de.braintags.vertx.jomnigate.annotation.field.Encoder;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class MemberEncrypted extends Member {

  @Encoder(name = "StandardEncoder")
  public String passwordEnc;

}
