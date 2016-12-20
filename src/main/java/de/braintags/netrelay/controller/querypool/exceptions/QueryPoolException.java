package de.braintags.netrelay.controller.querypool.exceptions;

import de.braintags.netrelay.controller.querypool.QueryPoolController;
import io.vertx.core.json.JsonObject;

/**
 * Abstract class for exceptions thrown from the {@link QueryPoolController} or connected parts<br>
 * <br>
 * Copyright: Copyright (c) 14.12.2016 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author sschmitt
 */

public abstract class QueryPoolException extends Exception {

  private static final long serialVersionUID = 1L;
  private JsonObject        query;

  /**
   * Creates an exception with only a message
   * 
   * @param message
   */
  public QueryPoolException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the message and the underlying query object
   * 
   * @param message
   * @param query
   */
  public QueryPoolException(String message, JsonObject query) {
    super(message);
    this.query = query;
  }

  /**
   * Creates an exception with the message, the underlying query object, and a root cause
   * 
   * @param message
   * @param query
   * @param cause
   */
  public QueryPoolException(String message, JsonObject query, Throwable cause) {
    super(message, cause);
    this.query = query;
  }

  /**
   * To be used to add the underlying query object after the creation of the exception
   * 
   * @param query
   *          the query to set
   */
  public void setQuery(JsonObject query) {
    this.query = query;
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Throwable#getMessage()
   */
  @Override
  public String getMessage() {
    String message = super.getMessage();
    if (query != null) {
      message += "\nSource query:\n" + query.encodePrettily();
    }
    return message;
  }

}
