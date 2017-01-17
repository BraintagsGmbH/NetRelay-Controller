package de.braintags.netrelay.controller.querypool.mapper;

import de.braintags.io.vertx.pojomapper.annotation.Entity;
import de.braintags.io.vertx.pojomapper.annotation.field.Embedded;
import de.braintags.netrelay.controller.querypool.TQueryPoolController;
import de.braintags.netrelay.model.AbstractRecord;

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
