package com.dpvn.crm.user;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.entity.User;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.ObjectUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
  private final CrmCrudClient crmCrudClient;

  public UserService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public List<UserDto> getByUser(Long userId) {
    List<UserDto> userDtos = crmCrudClient.getUsers().stream().filter(ud -> ud.getStatus() == 1).toList();
    UserDto user = userDtos.stream().filter(userDto -> userDto.getId().equals(userId)).findFirst().orElse(null);
    if (user == null) {
      return new ArrayList<>();
    }

    if (isGod(user)) {
      return userDtos;
    }

    if (isAdmin(user)) {
      return userDtos.stream().filter(ut -> !isGod(ut)).toList();
    }
    return userDtos.stream().filter(ut -> ObjectUtil.equals(ut.getDepartmentId(), user.getDepartmentId())).toList();
  }

  private boolean isGod(UserDto userDto) {
    return "ADMIN".equals(userDto.getRoleDto().getRoleName()) && "BOM".equals(userDto.getDepartmentDto().getDepartmentName());
  }

  private boolean isAdmin(UserDto userDto) {
    return "ADMIN".equals(userDto.getRoleDto().getRoleName());
  }

}
