package de.braintags.netrelay.model;

import java.util.ArrayList;
import java.util.List;

import de.braintags.io.vertx.pojomapper.annotation.Entity;
import de.braintags.io.vertx.pojomapper.annotation.field.Embedded;

@Entity
public class Country extends AbstractRecord {
  public String name;

  @Embedded
  public List<City> cities = new ArrayList<City>();

}