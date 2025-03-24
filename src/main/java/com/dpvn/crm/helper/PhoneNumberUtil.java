package com.dpvn.crm.helper;

public class PhoneNumberUtil {
  private PhoneNumberUtil() {}

  public static String maskPhoneNumber(String phone) {
    if (phone == null || phone.length() <= 3) {
      return phone;
    }
    StringBuilder maskedPhone = new StringBuilder();
    int digitCount = 0;
    for (char ch : phone.toCharArray()) {
      if (Character.isDigit(ch)) {
        if (digitCount < phone.length() - 3) {
          maskedPhone.append('x');
        } else {
          maskedPhone.append(ch);
        }
        digitCount++;
      } else {
        maskedPhone.append(ch);
      }
    }
    return maskedPhone.toString();
  }
}
