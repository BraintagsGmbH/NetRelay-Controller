package de.braintags.netrelay.model;

import de.braintags.io.vertx.pojomapper.annotation.Entity;

@Entity
public class Street extends AbstractRecord {
  public String name;

}