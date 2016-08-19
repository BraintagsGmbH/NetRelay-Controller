package de.braintags.netrelay.model;

import java.util.ArrayList;
import java.util.List;

import de.braintags.io.vertx.pojomapper.annotation.Entity;
import de.braintags.io.vertx.pojomapper.annotation.field.Embedded;

@Entity
public class TestCustomer extends Member {

  @Embedded
  private List<TestPhone> phoneNumbers = new ArrayList<TestPhone>();

  /**
   * @return the phoneNumbers
   */
  public List<TestPhone> getPhoneNumbers() {
    return phoneNumbers;
  }

  /**
   * @param phoneNumbers
   *          the phoneNumbers to set
   */
  public void setPhoneNumbers(List<TestPhone> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "TestCustomer [getFirstName()=" + getFirstName() + ", getLastName()=" + getLastName() + ", getId()="
        + getId() + "]";
  }

}