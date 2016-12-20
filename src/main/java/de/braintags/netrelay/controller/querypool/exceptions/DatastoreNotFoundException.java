package de.braintags.netrelay.controller.querypool.exceptions;

import de.braintags.io.vertx.pojomapper.IDataStore;
import io.vertx.core.json.JsonObject;

/**
 * To be thrown if a native query doesn't define a query for the currently used {@link IDataStore}<br>
 * <br>
 * Copyright: Copyright (c) 14.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */

public class DatastoreNotFoundException extends QueryPoolException {

  private static final long serialVersionUID = 1L;

  public DatastoreNotFoundException(String message) {
    super(message);
  }

  public DatastoreNotFoundException(String message, JsonObject query) {
    super(message, query);
  }

  public DatastoreNotFoundException(String message, JsonObject query, Throwable cause) {
    super(message, query, cause);
  }

}
