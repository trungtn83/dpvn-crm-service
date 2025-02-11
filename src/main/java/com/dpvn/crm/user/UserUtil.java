package com.dpvn.crm.user;

import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.UserDto;

public class UserUtil {
  private UserUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static boolean isGod(UserDto userDto) {
    return userDto.getActive().equals(Boolean.TRUE)
        && !userDto.getDeleted().equals(Boolean.TRUE)
        && Users.Role.GOD.equals(userDto.getRole().getRoleName())
        && Users.Department.BOM.equals(userDto.getDepartment().getDepartmentName());
  }

  public static boolean isDemiGod(UserDto userDto) {
    return isGod(userDto)
        || (userDto.getActive().equals(Boolean.TRUE)
            && !userDto.getDeleted().equals(Boolean.TRUE)
            && Users.Role.DEMIGOD.equals(userDto.getRole().getRoleName()));
  }
}
