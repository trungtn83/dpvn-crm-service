package com.dpvn.crm.hrm.leave;

import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hrm/leave-request")
public class LeaveRequestController {
  private final LeaveRequestService leaveRequestService;

  public LeaveRequestController(LeaveRequestService leaveRequestService) {
    this.leaveRequestService = leaveRequestService;
  }

  @PostMapping
  public void createLeaveRequest(@RequestBody LeaveRequestDto leaveRequestDto) {
    leaveRequestService.createLeaveRequest(leaveRequestDto);
  }

  @GetMapping("/find-by-user/{userId}")
  public List<LeaveRequestDto> findByUser(@PathVariable(name = "userId") Long userId) {
    return leaveRequestService.findByUserId(userId);
  }

  @GetMapping("/find-by-users")
  public List<LeaveRequestDto> findLeaveRequestByUsersAndInMonthOfDate(
      @RequestParam List<Long> userIds, @RequestParam(required = false) String date) {
    return leaveRequestService.findByUserIdsAndCurrentMonth(userIds, date);
  }
}
