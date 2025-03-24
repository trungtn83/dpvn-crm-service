package com.dpvn.crm.report;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.helper.PhoneNumberUtil;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.constant.Users;
import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.crmcrudservice.domain.dto.UserPropertyDto;
import com.dpvn.crmcrudservice.domain.entity.report.CustomerBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.InteractionBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.TaskBySeller;
import com.dpvn.reportcrudservice.domain.report.CallLogBySeller;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.wmscrudservice.domain.entity.report.InvoiceBySeller;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

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

    // calling paralell for performance improvement
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
        userDtos.stream().map(this::getVoip24hCodeForBySeller).filter(Objects::nonNull).toList();
    Map<String, List<CallLogBySeller>> callLogsBySellerMap =
        reportCrudClient.reportCallLogsBySellers(
            FastMap.create()
                .add("sellerVoip24hCodes", sellerVoip24hCodes)
                .add("fromDate", fromDate)
                .add("toDate", toDate));

    return userDtos.stream()
        .map(
            userDto ->
                reportSaleDetail(
                    loginUserDto,
                    userDto,
                    invoicesBySellerMap.getOrDefault(userDto.getIdf(), List.of()),
                    customersBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    interactionsBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    tasksBySellerMap.getOrDefault(userDto.getId(), List.of()),
                    callLogsBySellerMap.getOrDefault(
                        getVoip24hCodeForBySeller(userDto), List.of())))
        .toList();
  }

  /**
   * fromDate: String in format of LocalDate 2025-01-23
   * toDate: String in format of LocalDate 2025-01-25
   */
  private FastMap reportSaleDetail(
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

  private String getVoip24hCodeForBySeller(UserDto sellerDto) {
    if (ListUtil.isEmpty(sellerDto.getProperties())) {
      return null;
    }
    UserPropertyDto voip24hPropertyDto =
        sellerDto.getProperties().stream()
            .filter(p -> Users.Property.VOIP24H.equals(p.getCode()))
            .findFirst()
            .orElse(null);
    if (voip24hPropertyDto == null) {
      return null;
    }
    return voip24hPropertyDto.getValue();
  }
}
