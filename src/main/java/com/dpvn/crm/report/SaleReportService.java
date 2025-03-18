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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    Date now = new Date();
    if (userService.isGod(loginUserDto)
        || saleDto.getId().equals(loginUserDto.getId())
        || loginUserDto.getMembers().stream().anyMatch(u -> u.getId().equals(saleDto.getId()))) {
      saleDto.setPassword(null);
      FastMap result =
          FastMap.create()
              .add("sale", saleDto)
              .add("revenue", reportRevenue(saleDto.getIdf(), fromDateStr, toDateStr))
              .add("customer", reportCustomer(saleDto.getId(), fromDateStr, toDateStr))
              .add("task", reportTask(saleDto.getId(), fromDateStr, toDateStr))
              .add("voip24h", reportVoip24h(saleDto, fromDateStr, toDateStr));
      long diff = (new Date()).getTime() - now.getTime();
      LOGGER.info(
          "XXX: [{}-{}-{}-{}]] = {}",
          loginUserDto.getId(),
          saleDto.getId(),
          fromDateStr,
          toDateStr,
          diff);
      return result;
    }
    throw new BadRequestException("Can not view this sale");
  }

  @LogExecutionTime
  public FastMap reportRevenue(Long saleId, String fromDateStr, String toDateStr) {
    //    List<InvoiceDto> invoiceDtos =
    //        wmsCrudClient
    //            .findInvoicesByOptions(
    //                FastMap.create()
    //                    .add("sellerId", saleId)
    //                    .add("from", fromDateStr)
    //                    .add("to", toDateStr))
    //            .getRows();
    //    return FastMap.create()
    //        .add("total", invoiceDtos.size())
    //        .add("totalRevenue", invoiceDtos.stream().mapToLong(InvoiceDto::getTotal).sum());
    return wmsCrudClient.reportInvoicesBySeller(
        FastMap.create()
            .add("sellerId", saleId)
            .add("fromDate", fromDateStr)
            .add("toDate", toDateStr));
  }

  private FastMap reportCustomer(Long saleId, String fromDateStr, String toDateStr) {
    //    List<SaleCustomerDto> saleCustomerDtos =
    //        crmCrudClient.findSaleCustomersBySale(
    //            FastMap.create()
    //                .add("saleId", saleId)
    //                .add("fromDate", fromDateStr)
    //                .add("toDate", toDateStr));
    //    List<CustomerDto> customerDtos =
    //        saleCustomerDtos.stream().map(SaleCustomerDto::getCustomerDto).toList();
    //    Map<Long, CustomerDto> customerMapById =
    //        customerDtos.stream().collect(Collectors.toMap(BaseDto::getId, i -> i, (i1, i2) ->
    // i2));
    //
    //    Instant fromDate = DateUtil.from(LocalDateUtil.from(fromDateStr));
    //    Instant toDate = DateUtil.from(LocalDateUtil.from(toDateStr));
    //    long selfDig =
    //        customerMapById.values().stream()
    //            .filter(
    //                c ->
    //                    saleId.equals(c.getCreatedBy())
    //                        && c.getCreatedDate().isAfter(fromDate)
    //                        && c.getCreatedDate().isBefore(toDate))
    //            .count();

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
    //    List<InteractionDto> interactionDtos =
    // crmCrudClient.findAllInteractions(allInteractionBody);
    Long takeCare = crmCrudClient.countReportInteractionBySeller(allInteractionBody);
    return result.add("takeCare", takeCare);
  }

  private FastMap reportTask(Long saleId, String fromDate, String toDate) {
    List<TaskDto> findTasksReportBySeller =
        taskService.findTasksReportBySeller(saleId, fromDate, toDate);
    return FastMap.create().add("total", findTasksReportBySeller.size());
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
