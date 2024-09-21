package com.dpvn.crm.hrm.leave;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.shared.util.DateUtil;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaveRequestService {
  private final CrmCrudClient crmCrudClient;

  public LeaveRequestService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public void createLeaveRequest(LeaveRequestDto leaveRequestDto) {
    crmCrudClient.createLeaveRequest(leaveRequestDto);
  }

  public List<LeaveRequestDto> findByUserId(Long userId) {
    return crmCrudClient.findLeaveRequestByUserId(userId);
  }

  public List<LeaveRequestDto> findByUserIdsAndCurrentMonth(List<Long> userIds, String date) {
    Instant current = DateUtil.from(date, DateUtil.now());
    return crmCrudClient.findLeaveRequestByUsersAndInMonthOfDate(
        userIds,
        DateUtil.startOfMonth(current).toString(),
        DateUtil.endOfMonth(current).toString());
  }
}
