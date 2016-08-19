package de.braintags.netrelay.model;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TestPhone extends AbstractRecord {
  private String phoneNumber;

  public TestPhone() {
  }

  public TestPhone(String phone) {
    this.phoneNumber = phone;
  }

  /**
   * @return the phoneNumber
   */
  public String getPhoneNumber() {
    return phoneNumber;
  }

  /**
   * @param phoneNumber
   *          the phoneNumber to set
   */
  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

}
