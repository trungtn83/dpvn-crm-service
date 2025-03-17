package com.dpvn.crm.user;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
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
    UserDto dbUserDto = crmCrudClient.createNewUser(userDto);
    updateMember(dbUserDto, userDto.getMemberIds());
  }

  public void updateUser(Long id, FastMap userDto) {
    UserDto dbUserDto = crmCrudClient.updateExistedUser(id, userDto);
    updateMember(dbUserDto, userDto.getListClass("memberIds", Long.class));
  }

  private void updateMember(UserDto dbUserDto, List<Long> memberIds) {
    List<Long> dbMemberIds = dbUserDto.getMembers().stream().map(UserDto::getId).toList();
    memberIds.forEach(
        memberId -> {
          if (!dbMemberIds.contains(memberId)) {
            crmCrudClient.updateMember(
                FastMap.create()
                    .add("leaderId", dbUserDto.getId())
                    .add("memberId", memberId)
                    .add("action", Users.Action.ADD));
          }
        });
    dbMemberIds.forEach(
        dbMemberId -> {
          if (!memberIds.contains(dbMemberId)) {
            crmCrudClient.updateMember(
                FastMap.create()
                    .add("leaderId", dbUserDto.getId())
                    .add("memberId", dbMemberId)
                    .add("action", Users.Action.REMOVE));
          }
        });
  }

  public void deleteUser(Long id) {
    crmCrudClient.deleteUser(id);
  }

  public boolean isGod(Long userId) {
    UserDto user = findById(userId);
    return UserUtil.isGod(user);
  }

  public boolean isGod(UserDto user) {
    return UserUtil.isGod(user);
  }
}
