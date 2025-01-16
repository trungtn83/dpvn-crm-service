package com.dpvn.crm.user;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.util.FastMap;
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

  public UserDto findById(Long userId) {
    return crmCrudClient.getUserById(userId);
  }

  public List<UserDto> findUsersByLeaderId(Long userId) {
    List<UserDto> userDtos = crmCrudClient.getUsers().stream().filter(UserDto::getActive).toList();
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

  public boolean isGod(UserDto userDto) {
    return "GOD".equals(userDto.getRole().getRoleName());
  }

  private boolean isAdmin(UserDto userDto) {
    return "ADMIN".equals(userDto.getRole().getRoleName())
        && "BOM".equals(userDto.getDepartment().getDepartmentName());
  }

  public List<UserDto> findUsersByIds(List<Long> userIds) {
    return crmCrudClient.findUsersByIds(userIds);
  }

  public Map<Long, String> findUsersByIdsMapByIds(List<Long> userIds) {
    return findUsersByIds(userIds).stream()
        .collect(Collectors.toMap(UserDto::getId, UserDto::getFullName));
  }

  public UserDto findUserByKvUserId(Long kvUserId) {
    UserDto userOption = new UserDto();
    userOption.setIdf(kvUserId);
    return findUsersByOptions(userOption).stream().findFirst().orElseThrow();
  }

  public List<UserDto> findUsersByOptions(UserDto userDto) {
    return crmCrudClient.findUsersByOptions(userDto);
  }

  public FastMap searchUsers(FastMap condition) {
    return crmCrudClient.searchUsers(condition);
  }
}
