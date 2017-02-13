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
package de.braintags.netrelay.controller.querypool.exceptions;

import io.vertx.core.json.JsonObject;

/**
 * An exception to be thrown if the given query JSON has an invalid syntax<br>
 * <br>
 * Copyright: Copyright (c) 13.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */

public class InvalidSyntaxException extends QueryPoolException {

  private static final long serialVersionUID = 1L;

  public InvalidSyntaxException(String message) {
    super(message);
  }

  public InvalidSyntaxException(String message, JsonObject query) {
    super(message, query);
  }

  public InvalidSyntaxException(String message, JsonObject query, Throwable cause) {
    super(message, query, cause);
  }

}
