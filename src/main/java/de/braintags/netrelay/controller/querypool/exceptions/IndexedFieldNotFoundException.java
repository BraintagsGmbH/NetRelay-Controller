package de.braintags.netrelay.controller.querypool.exceptions;

import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;

/**
 * Thrown when a field of a dynamic query could not be matched to an {@link IIndexedField}
 * 
 * @author sschmitt
 * 
 */
public class IndexedFieldNotFoundException extends QueryPoolException {

  private static final long serialVersionUID = -8343659821242968318L;

  public IndexedFieldNotFoundException(String message, Exception cause) {
    super(message, cause);
  }

}
