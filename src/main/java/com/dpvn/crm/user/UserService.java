package com.dpvn.crm.user;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.dto.UserPropertyDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  public PagingResponse<UserDto> listAllUsers() {
    return crmCrudClient.getUsers(-1, -1);
  }

  public void createNewUser(UserDto userDto) {
    crmCrudClient.createNewUser(userDto);
  }

  public void updateUser(Long id, FastMap userDto) {
    List<UserPropertyDto> userPropertyDtos =
        userDto.getListClass("properties", UserPropertyDto.class);
    List<UserPropertyDto> discipleUserPropertyDto =
        userDto.getListClass("memberIds", Long.class).stream()
            .map(
                discipleId -> {
                  UserPropertyDto dto = new UserPropertyDto();
                  dto.setCode("MEMBER");
                  dto.setStatus("DISCIPLE");
                  dto.setValue(discipleId.toString());
                  return dto;
                })
            .toList();
    List<UserPropertyDto> judasUserPropertyDtos =
        userDto.getListClass("transferIds", Long.class).stream()
            .map(
                judasId -> {
                  UserPropertyDto dto = new UserPropertyDto();
                  dto.setCode("MEMBER");
                  dto.setStatus("JUDAS");
                  dto.setValue(judasId.toString());
                  return dto;
                })
            .toList();

    List<UserPropertyDto> properties =
        Stream.of(userPropertyDtos, discipleUserPropertyDto, judasUserPropertyDtos)
            .flatMap(List::stream)
            .toList();
    userDto.add("properties", properties);
    crmCrudClient.updateExistedUser(id, userDto);
  }

  public boolean isGod(Long userId) {
    UserDto user = findById(userId);
    return UserUtil.isGod(user);
  }

  public boolean isGod(UserDto user) {
    return UserUtil.isGod(user);
  }

  public List<UserDto> getSaleUsersUnder(UserDto userDto) {
    if (isGod(userDto)) {
      return getSaleUsers();
    }
    List<UserDto> userDtos = findUsersByIds(userDto.getDiscipleMemberIds());
    userDtos.add(userDto);
    return userDtos;
  }

  public List<UserDto> getSaleUsers() {
    return listAllUsers().getRows().stream()
        .filter(
            u ->
                u.getActive()
                    && (u.getDepartment() != null
                        && u.getDepartment().getDepartmentName().equals(Users.Department.SALE)))
        .toList();
  }
}
