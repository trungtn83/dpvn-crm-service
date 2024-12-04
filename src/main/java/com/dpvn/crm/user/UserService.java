package com.dpvn.crm.user;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.reportcrudservice.domain.dto.kiotviet.KvUserDto;
import com.dpvn.shared.util.ObjectUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final CrmCrudClient crmCrudClient;
  private final ReportCrudClient reportCrudClient;

  public UserService(CrmCrudClient crmCrudClient, ReportCrudClient reportCrudClient) {
    this.crmCrudClient = crmCrudClient;
    this.reportCrudClient = reportCrudClient;
  }

  public List<UserDto> getByUser(Long userId) {
    List<UserDto> userDtos =
        crmCrudClient.getUsers().stream().filter(ud -> ud.getStatus() == 1).toList();
    UserDto user =
        userDtos.stream()
            .filter(userDto -> userDto.getId().equals(userId))
            .findFirst()
            .orElse(null);
    if (user == null) {
      return new ArrayList<>();
    }

    if (isGod(user)) {
      return userDtos;
    }

    if (isAdmin(user)) {
      return userDtos.stream().filter(ut -> !isGod(ut)).toList();
    }
    return userDtos.stream()
        .filter(ut -> ObjectUtil.equals(ut.getDepartmentId(), user.getDepartmentId()))
        .toList();
  }

  private boolean isGod(UserDto userDto) {
    return "ADMIN".equals(userDto.getRoleDto().getRoleName())
        && "BOM".equals(userDto.getDepartmentDto().getDepartmentName());
  }

  private boolean isAdmin(UserDto userDto) {
    return "ADMIN".equals(userDto.getRoleDto().getRoleName());
  }

  public List<UserDto> findUsersByIds(List<Long> userIds) {
    return crmCrudClient.findUsersByIds(userIds);
  }

  public Map<Long, String> findUsersByIdsMapByIds(List<Long> userIds) {
    return findUsersByIds(userIds).stream()
        .collect(Collectors.toMap(UserDto::getId, UserDto::getFullName));
  }

  public UserDto findUserByKvUserId(Long kvUserId) {
    KvUserDto kvUserDto = reportCrudClient.findKvUserById(kvUserId);
    return crmCrudClient.getCrmUserByUserName(kvUserDto.getUsername());
  }
}
