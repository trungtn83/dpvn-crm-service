package com.dpvn.crm.report;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.helper.PhoneNumberUtil;
import com.dpvn.crm.report.domain.PerformanceBySellerDetail;
import com.dpvn.crm.user.UserService;
import com.dpvn.crm.user.UserUtil;
import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.entity.report.CustomerBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.InteractionBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.TaskBySeller;
import com.dpvn.reportcrudservice.domain.report.CallLogBySeller;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.LocalDateUtil;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.wmscrudservice.domain.entity.report.InvoiceBySeller;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SellerReportService extends AbstractService {
  private final WmsCrudClient wmsCrudClient;
  private final CrmCrudClient crmCrudClient;
  private final UserService userService;
  private final ReportCrudClient reportCrudClient;

  public SellerReportService(
      WmsCrudClient wmsCrudClient,
      CrmCrudClient crmCrudClient,
      UserService userService,
      ReportCrudClient reportCrudClient) {
    this.wmsCrudClient = wmsCrudClient;
    this.crmCrudClient = crmCrudClient;
    this.userService = userService;
    this.reportCrudClient = reportCrudClient;
  }

  public List<FastMap> reportSales(Long loginUserId, String fromDate, String toDate) {
    UserDto loginUserDto = userService.findById(loginUserId);
    List<UserDto> userDtos = userService.getUserMembers(loginUserDto);

    List<Long> sellerIdfs = userDtos.stream().map(UserDto::getIdf).toList();
    Map<Long, List<InvoiceBySeller>> invoicesBySellerMap =
        wmsCrudClient.reportInvoicesBySellers(
            FastMap.create()
                .add("sellerIds", sellerIdfs)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    List<Long> sellerIds = userDtos.stream().map(UserDto::getId).toList();
    Map<Long, List<CustomerBySeller>> customersBySellerMap =
        crmCrudClient.reportCustomersBySellers(
            FastMap.create()
                .add("sellerIds", sellerIds)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    Map<Long, List<InteractionBySeller>> interactionsBySellerMap =
        crmCrudClient.reportInteractionsBySellers(
            FastMap.create()
                .add("sellerIds", sellerIds)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    Map<Long, List<TaskBySeller>> tasksBySellerMap =
        crmCrudClient.reportTasksBySellers(
            FastMap.create()
                .add("sellerIds", sellerIds)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    List<String> sellerVoip24hCodes =
        userDtos.stream()
            .map(UserUtil::getVoip24hCodeForBySeller)
            .filter(Objects::nonNull)
            .toList();
    Map<String, List<CallLogBySeller>> callLogsBySellerMap =
        reportCrudClient.reportCallLogsBySellers(
            FastMap.create()
                .add("sellerVoip24hCodes", sellerVoip24hCodes)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    return userDtos.stream()
        .map(
            userDto ->
                reportSales(
                    loginUserDto,
                    userDto,
                    invoicesBySellerMap.getOrDefault(userDto.getIdf(), List.of()),
                    customersBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    interactionsBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    tasksBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    callLogsBySellerMap.getOrDefault(
                        UserUtil.getVoip24hCodeForBySeller(userDto), List.of())))
        .toList();
  }

  /**
   * fromDate: String in format of LocalDate 2025-01-23
   * toDate: String in format of LocalDate 2025-01-25
   */
  private FastMap reportSales(
      UserDto loginUserDto,
      UserDto sellerDto,
      List<InvoiceBySeller> invoicesBySeller,
      List<CustomerBySeller> customersBySeller,
      List<InteractionBySeller> interactionsBySeller,
      List<TaskBySeller> tasksBySeller,
      List<CallLogBySeller> callLogsBySeller) {
    if (userService.isGod(loginUserDto)
        || sellerDto.getId().equals(loginUserDto.getId())
        || loginUserDto.getMembers().stream().anyMatch(u -> u.getId().equals(sellerDto.getId()))) {
      sellerDto.setPassword(null);

      interactionsBySeller.forEach(
          i -> {
            if (i.getVisibility() == Visibility.PRIVATE
                && !loginUserDto.getId().equals(i.getSellerId())) {
              i.setContent(null);
            }
          });

      if (!userService.isGod(loginUserDto) && !sellerDto.getId().equals(loginUserDto.getId())) {
        customersBySeller.forEach(
            c -> c.setMobilePhone(PhoneNumberUtil.maskPhoneNumber(c.getMobilePhone())));
        interactionsBySeller.forEach(
            i -> i.setMobilePhone(PhoneNumberUtil.maskPhoneNumber(i.getMobilePhone())));
        callLogsBySeller.forEach(
            c -> {
              c.setCaller(PhoneNumberUtil.maskPhoneNumber(c.getCaller()));
              c.setCallee(PhoneNumberUtil.maskPhoneNumber(c.getCallee()));
            });
      }

      return FastMap.create()
          .add("seller", sellerDto)
          .add("invoices", invoicesBySeller)
          .add("customers", customersBySeller)
          .add("interactions", interactionsBySeller)
          .add("tasks", tasksBySeller)
          .add("callLogs", callLogsBySeller);
    }

    throw new BadRequestException("Can not view this sale");
  }

  public List<PerformanceBySellerDetail> reportSaleDetail(
      Long loginUserId, Long sellerId, String fromDateStr, String toDateStr) {
    List<UserDto> userDtos = userService.findUsersByIds(List.of(loginUserId, sellerId));
    UserDto loginUserDto =
        userDtos.stream().filter(u -> u.getId().equals(loginUserId)).findFirst().orElse(null);
    UserDto sellerDto =
        userDtos.stream().filter(u -> u.getId().equals(sellerId)).findFirst().orElse(null);
    if (loginUserDto == null
        || sellerDto == null
        || !UserUtil.isReportable(loginUserDto, sellerDto)) {
      throw new BadRequestException("Can not view this sale");
    }

    sellerDto.setPassword(null);
    return reportPerformanceBySellerDetail(sellerDto, fromDateStr, toDateStr);
  }

  private List<PerformanceBySellerDetail> reportPerformanceBySellerDetail(
      UserDto sellerDto, String fromDateStr, String toDateStr) {
    Long sellerIdf = sellerDto.getIdf();
    List<Long> sellerIdfs = List.of(sellerIdf);
    List<InvoiceBySeller> invoices =
        wmsCrudClient
            .reportInvoicesBySellers(
                FastMap.create()
                    .add("sellerIds", sellerIdfs)
                    .add("fromDate", fromDateStr)
                    .add("toDate", toDateStr))
            .getOrDefault(sellerIdf, List.of());
    Map<LocalDate, List<InvoiceBySeller>> invoicesBySellerMapByLocalDate =
        invoices.stream()
            .collect(
                Collectors.groupingBy(invoice -> LocalDateUtil.from(invoice.getPurchaseDate())));

    Long sellerId = sellerDto.getId();
    List<Long> sellerIds = List.of(sellerDto.getId());
    List<CustomerBySeller> customers =
        crmCrudClient
            .reportCustomersBySellers(
                FastMap.create()
                    .add("sellerIds", sellerIds)
                    .add("fromDate", fromDateStr)
                    .add("toDate", toDateStr))
            .getOrDefault(sellerId, List.of());
    Map<LocalDate, List<CustomerBySeller>> customersBySellerMapByLocalDate =
        customers.stream()
            .collect(
                Collectors.groupingBy(customer -> LocalDateUtil.from(customer.getCreatedDate())));

    List<InteractionBySeller> interactions =
        crmCrudClient
            .reportInteractionsBySellers(
                FastMap.create()
                    .add("sellerIds", sellerIds)
                    .add("fromDate", fromDateStr)
                    .add("toDate", toDateStr))
            .getOrDefault(sellerId, List.of());
    Map<LocalDate, List<InteractionBySeller>> interactionsBySellerMapByLocalDate =
        interactions.stream()
            .collect(
                Collectors.groupingBy(
                    interaction -> LocalDateUtil.from(interaction.getCreatedDate())));

    List<TaskBySeller> tasks =
        crmCrudClient
            .reportTasksBySellers(
                FastMap.create()
                    .add("sellerIds", sellerIds)
                    .add("fromDate", fromDateStr)
                    .add("toDate", toDateStr))
            .getOrDefault(sellerId, List.of());
    Map<LocalDate, List<TaskBySeller>> tasksBySellerByLocalDate =
        tasks.stream()
            .collect(Collectors.groupingBy(task -> LocalDateUtil.from(task.getModifiedDate())));

    String voip24hSellerCode = UserUtil.getVoip24hCodeForBySeller(sellerDto);
    Map<LocalDate, List<CallLogBySeller>> callLogsBySellerByLocalDate =
        StringUtil.isEmpty(voip24hSellerCode)
            ? Map.of()
            : getCallLogsBySellerByLocalDate(voip24hSellerCode, fromDateStr, toDateStr);

    Set<LocalDate> allDates = new HashSet<>();
    allDates.addAll(invoicesBySellerMapByLocalDate.keySet());
    allDates.addAll(customersBySellerMapByLocalDate.keySet());
    allDates.addAll(interactionsBySellerMapByLocalDate.keySet());
    allDates.addAll(tasksBySellerByLocalDate.keySet());
    allDates.addAll(callLogsBySellerByLocalDate.keySet());

    return allDates.stream()
        .map(
            date -> {
              PerformanceBySellerDetail performanceBySellerDetail = new PerformanceBySellerDetail();
              performanceBySellerDetail.setDay(date);
              performanceBySellerDetail.setInvoices(
                  invoicesBySellerMapByLocalDate.getOrDefault(date, List.of()));
              performanceBySellerDetail.setCustomers(
                  customersBySellerMapByLocalDate.getOrDefault(date, List.of()));
              performanceBySellerDetail.setInteractions(
                  interactionsBySellerMapByLocalDate.getOrDefault(date, List.of()));
              performanceBySellerDetail.setTasks(
                  tasksBySellerByLocalDate.getOrDefault(date, List.of()));
              performanceBySellerDetail.setCallLogs(
                  callLogsBySellerByLocalDate.getOrDefault(date, List.of()));
              return performanceBySellerDetail;
            })
        .toList();
  }

  private Map<LocalDate, List<CallLogBySeller>> getCallLogsBySellerByLocalDate(
      String voip24hSellerCode, String fromDateStr, String toDateStr) {
    List<CallLogBySeller> callLogs =
        reportCrudClient
            .reportCallLogsBySellers(
                FastMap.create()
                    .add("sellerVoip24hCodes", List.of(voip24hSellerCode))
                    .add("fromDate", fromDateStr)
                    .add("toDate", toDateStr))
            .getOrDefault(voip24hSellerCode, List.of());
    return callLogs.stream()
        .collect(Collectors.groupingBy(callLog -> LocalDateUtil.from(callLog.getCallDate())));
  }
}
