package com.dpvn.crm.client;

import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import java.util.List;

import com.dpvn.crmcrudservice.domain.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "crm-crud-service", contextId = "crm-crud-service-client")
public interface CrmCrudClient {
  @GetMapping("/user/{id}")
  UserDto getUserById(@PathVariable("id") Long id);

  @GetMapping("/user")
  List<UserDto> getUsers();

  @PostMapping("/hrm/leave-request")
  void createLeaveRequest(@RequestBody LeaveRequestDto leaveRequestDto);

  @GetMapping("/hrm/leave-request/find-by-user/{userId}")
  List<LeaveRequestDto> findLeaveRequestByUserId(@PathVariable(name = "userId") Long userId);

  @GetMapping("/hrm/leave-request/find-by-users")
  List<LeaveRequestDto> findLeaveRequestByUsersAndInMonthOfDate(
      @RequestParam(name = "userIds") List<Long> userIds, @RequestParam(name = "fromDate") String fromDate, @RequestParam(name = "toDate") String toDate);
}
