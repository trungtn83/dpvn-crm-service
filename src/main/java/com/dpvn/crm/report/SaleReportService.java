package com.dpvn.crm.report;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.task.TaskService;
import com.dpvn.crm.user.UserService;
import com.dpvn.crm.voip24h.domain.ViCallLogDto;
import com.dpvn.crm.voip24h.domain.ViCallLogs;
import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.dto.*;
import com.dpvn.shared.domain.BaseDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.LocalDateUtil;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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
    List<UserDto> userDtos = loginUserDto.getMembers();
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
        .map(userDto -> reportSaleDetail(loginUserId, userDto.getId(), fromDate, toDate))
        .toList();
  }

  /**
   * fromDate: String in format of LocalDate 2025-01-23
   * toDate: String in format of LocalDate 2025-01-25
   */
  public FastMap reportSaleDetail(
      Long loginUserId, Long saleId, String fromDateStr, String toDateStr) {
    UserDto loginUserDto = userService.findById(loginUserId);
    if (userService.isGod(loginUserDto)
        || saleId.equals(loginUserId)
        || loginUserDto.getMembers().stream().anyMatch(u -> u.getId().equals(saleId))) {
      UserDto sale = userService.findById(saleId);
      sale.setPassword(null);
      return FastMap.create()
          .add("sale", sale)
          .add("revenue", reportRevenue(sale.getIdf(), fromDateStr, toDateStr))
          .add("customer", reportCustomer(saleId, fromDateStr, toDateStr))
          .add("task", reportTask(saleId, fromDateStr, toDateStr))
          .add("voip24h", reportVoip24h(saleId, fromDateStr, toDateStr));
    }
    throw new BadRequestException("Can not view this sale");
  }

  private FastMap reportRevenue(Long saleId, String fromDateStr, String toDateStr) {
    List<InvoiceDto> invoiceDtos =
        wmsCrudClient
            .findInvoicesByOptions(
                FastMap.create()
                    .add("sellerId", saleId)
                    .add("from", fromDateStr)
                    .add("to", toDateStr))
            .getRows();
    return FastMap.create()
        .add("total", invoiceDtos.size())
        .add("totalRevenue", invoiceDtos.stream().mapToLong(InvoiceDto::getTotal).sum());
  }

  private FastMap reportCustomer(Long saleId, String fromDateStr, String toDateStr) {
    List<SaleCustomerDto> saleCustomerDtos =
        crmCrudClient.findSaleCustomersBySale(
            FastMap.create()
                .add("saleId", saleId)
                .add("fromDate", fromDateStr)
                .add("toDate", toDateStr));
    List<CustomerDto> customerDtos =
        saleCustomerDtos.stream().map(SaleCustomerDto::getCustomerDto).toList();
    Map<Long, CustomerDto> customerMapById =
        customerDtos.stream().collect(Collectors.toMap(BaseDto::getId, i -> i, (i1, i2) -> i2));

    Instant fromDate = DateUtil.from(LocalDateUtil.from(fromDateStr));
    Instant toDate = DateUtil.from(LocalDateUtil.from(toDateStr));
    long selfDig =
        customerMapById.values().stream()
            .filter(
                c ->
                    saleId.equals(c.getCreatedBy())
                        && c.getCreatedDate().isAfter(fromDate)
                        && c.getCreatedDate().isBefore(toDate))
            .count();

    FastMap allInteractionBody =
        FastMap.create()
            .add("userId", saleId)
            .add("fromDate", fromDateStr)
            .add("toDate", toDateStr);
    List<InteractionDto> interactionDtos = crmCrudClient.findAllInteractions(allInteractionBody);

    return FastMap.create()
        .add("selfDig", selfDig)
        .add("totalDig", customerMapById.size())
        .add("takeCare", interactionDtos.size());
  }

  private FastMap reportTask(Long saleId, String fromDate, String toDate) {
    List<TaskDto> findTasksReportBySeller =
        taskService.findTasksReportBySeller(saleId, fromDate, toDate);
    return FastMap.create().add("total", findTasksReportBySeller.size());
  }

  private FastMap reportVoip24h(Long saleId, String fromDateStr, String toDateStr) {
    UserDto sale = userService.findById(saleId);
    if (ListUtil.isEmpty(sale.getProperties())) {
      return FastMap.create();
    }
    UserPropertyDto voip24hPropertyDto =
        sale.getProperties().stream()
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
