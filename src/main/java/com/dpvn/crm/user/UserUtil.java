package com.dpvn.crm.user;

import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.dto.UserPropertyDto;
import com.dpvn.shared.util.ListUtil;

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

  public static boolean isAccount(UserDto userDto) {
    return isGod(userDto)
        || (userDto.getActive().equals(Boolean.TRUE)
            && !userDto.getDeleted().equals(Boolean.TRUE)
            && Users.Department.ACCOUNT.equals(userDto.getDepartment().getDepartmentName()));
  }

  public static boolean isAdmin(UserDto userDto) {
    return isGod(userDto)
        || (userDto.getActive().equals(Boolean.TRUE)
            && !userDto.getDeleted().equals(Boolean.TRUE)
            && Users.Department.ADMIN.equals(userDto.getDepartment().getDepartmentName()));
  }

  public static boolean isReportable(UserDto loginUserDto, UserDto sellerDto) {
    return isGod(loginUserDto)
        || loginUserDto.getId().equals(sellerDto.getId())
        || loginUserDto.getMembers().stream().anyMatch(u -> u.getId().equals(sellerDto.getId()));
  }

  public static String getVoip24hCodeForBySeller(UserDto sellerDto) {
    if (ListUtil.isEmpty(sellerDto.getProperties())) {
      return null;
    }
    UserPropertyDto voip24hPropertyDto =
        sellerDto.getProperties().stream()
            .filter(p -> Users.Property.VOIP24H.equals(p.getCode()))
            .findFirst()
            .orElse(null);
    if (voip24hPropertyDto == null) {
      return null;
    }
    return voip24hPropertyDto.getValue();
  }
}
