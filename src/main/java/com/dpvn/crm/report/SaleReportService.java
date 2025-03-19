package com.dpvn.crm.report;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.helper.LogExecutionTime;
import com.dpvn.crm.task.TaskService;
import com.dpvn.crm.user.UserService;
import com.dpvn.crm.voip24h.domain.ViCallLogDto;
import com.dpvn.crm.voip24h.domain.ViCallLogs;
import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.dto.UserPropertyDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SaleReportService extends AbstractService {
  private final WmsCrudClient wmsCrudClient;
  private final CrmCrudClient crmCrudClient;
  private final UserService userService;
  private final ReportCrudClient reportCrudClient;
  private final TaskService taskService;

  public SaleReportService(
      WmsCrudClient wmsCrudClient,
      CrmCrudClient crmCrudClient,
      UserService userService,
      ReportCrudClient reportCrudClient,
      TaskService taskService) {
    this.wmsCrudClient = wmsCrudClient;
    this.crmCrudClient = crmCrudClient;
    this.userService = userService;
    this.reportCrudClient = reportCrudClient;
    this.taskService = taskService;
  }

  public List<FastMap> reportSales(Long loginUserId, String fromDate, String toDate) {
    UserDto loginUserDto = userService.findById(loginUserId);
    List<UserDto> userDtos =
        new ArrayList<>(
            loginUserDto.getMembers()); // clone new one to avoid change loginUserDto.members() list
    if (!userService.isGod(loginUserDto)) {
      userDtos.add(0, loginUserDto);
    } else {
      userDtos.clear();
      List<UserDto> response = userService.listAllUsers().getRows();
      userDtos.addAll(
          response.stream()
              .filter(
                  u ->
                      u.getActive()
                          && (u.getDepartment() != null
                          && u.getDepartment()
                          .getDepartmentName()
                          .equals(Users.Department.SALE)))
              .toList());
    }
    return userDtos.stream()
        .map(userDto -> reportSaleDetail(loginUserDto, userDto, fromDate, toDate))
        .toList();
  }

  public FastMap reportSaleDetail(
      Long loginUserId, Long saleId, String fromDateStr, String toDateStr) {
    UserDto loginUserDto = userService.findById(loginUserId);
    UserDto saleDto =
        loginUserDto.getMembers().stream()
            .filter(u -> u.getId().equals(saleId))
            .findFirst()
            .orElse(null);
    if (saleDto == null) {
      throw new BadRequestException("Can not view this sale");
    }
    return reportSaleDetail(loginUserDto, saleDto, fromDateStr, toDateStr);
  }

  /**
   * fromDate: String in format of LocalDate 2025-01-23
   * toDate: String in format of LocalDate 2025-01-25
   */
  private FastMap reportSaleDetail(
      UserDto loginUserDto, UserDto saleDto, String fromDateStr, String toDateStr) {
    if (userService.isGod(loginUserDto)
        || saleDto.getId().equals(loginUserDto.getId())
        || loginUserDto.getMembers().stream().anyMatch(u -> u.getId().equals(saleDto.getId()))) {
      saleDto.setPassword(null);
      FastMap result =
          FastMap.create()
              .add("sale", saleDto)
              .add("revenue", reportRevenue(saleDto.getIdf(), fromDateStr, toDateStr))
              .add("customer", reportCustomer(saleDto.getId(), fromDateStr, toDateStr))
              .add("tasks", reportTask(saleDto.getId(), fromDateStr, toDateStr))
              .add("voip24h", reportVoip24h(saleDto, fromDateStr, toDateStr));
      return result;
    }
    throw new BadRequestException("Can not view this sale");
  }

  @LogExecutionTime
  public FastMap reportRevenue(Long saleId, String fromDateStr, String toDateStr) {
    return wmsCrudClient.reportInvoicesBySeller(
        FastMap.create()
            .add("sellerId", saleId)
            .add("fromDate", fromDateStr)
            .add("toDate", toDateStr));
  }

  private FastMap reportCustomer(Long saleId, String fromDateStr, String toDateStr) {
    FastMap result =
        crmCrudClient.findSaleCustomersBySale(
            FastMap.create()
                .add("saleId", saleId)
                .add("fromDate", fromDateStr)
                .add("toDate", toDateStr));
    FastMap allInteractionBody =
        FastMap.create()
            .add("userId", saleId)
            .add("fromDate", fromDateStr)
            .add("toDate", toDateStr);
    Long takeCare = crmCrudClient.countReportInteractionBySeller(allInteractionBody);
    return result.add("takeCare", takeCare);
  }

  private List<TaskDto> reportTask(Long saleId, String fromDate, String toDate) {
    return taskService.findTasksReportBySeller(saleId, fromDate, toDate);
  }

  private FastMap reportVoip24h(UserDto saleDto, String fromDateStr, String toDateStr) {
    if (ListUtil.isEmpty(saleDto.getProperties())) {
      return FastMap.create();
    }
    UserPropertyDto voip24hPropertyDto =
        saleDto.getProperties().stream()
            .filter(p -> Users.Property.VOIP24H.equals(p.getCode()))
            .findFirst()
            .orElse(null);
    if (voip24hPropertyDto == null) {
      return FastMap.create();
    }
    List<ViCallLogDto> callLogDtos =
        reportCrudClient.findCallLogsByCaller(
            voip24hPropertyDto.getValue(), fromDateStr, toDateStr);
    Long callIn =
        callLogDtos.stream().filter(cl -> ViCallLogs.Type.INBOUND.equals(cl.getType())).count();
    Long callOut =
        callLogDtos.stream().filter(cl -> ViCallLogs.Type.OUTBOUND.equals(cl.getType())).count();
    Long ringing = callLogDtos.stream().mapToLong(cl -> cl.getDuration() - cl.getBillSec()).sum();
    Long calling = callLogDtos.stream().mapToLong(ViCallLogDto::getBillSec).sum();
    return FastMap.create()
        .add("in", callIn)
        .add("out", callOut)
        .add("ringing", ringing)
        .add("calling", calling);
  }
}
