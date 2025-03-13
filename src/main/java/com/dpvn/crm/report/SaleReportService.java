package com.dpvn.crm.report;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.dto.*;
import com.dpvn.shared.domain.BaseDto;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
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

  public SaleReportService(
      WmsCrudClient wmsCrudClient, CrmCrudClient crmCrudClient, UserService userService) {
    this.wmsCrudClient = wmsCrudClient;
    this.crmCrudClient = crmCrudClient;
    this.userService = userService;
  }

  /**
   *  fromDate: String in format of LocalDate 2025-01-23
   *  toDate: String in format of LocalDate 2025-01-25
   */
  public FastMap report(Long saleId, String fromDateStr, String toDateStr) {
    UserDto sale = userService.findById(saleId);
    return FastMap.create()
        .add("sale", sale)
        .add("revenue", reportRevenue(saleId, fromDateStr, toDateStr))
        .add("customer", reportCustomer(saleId, fromDateStr, toDateStr))
        .add("task", reportTask(saleId, fromDateStr, toDateStr));
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
            FastMap.create().add("saleId", saleId).add("from", fromDateStr).add("to", toDateStr));
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
                    c.getCreatedBy().equals(saleId)
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
        crmCrudClient.findTasksReportBySeller(saleId, fromDate, toDate);
    return FastMap.create().add("total", findTasksReportBySeller.size());
  }
}
