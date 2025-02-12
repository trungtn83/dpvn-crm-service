package com.dpvn.crm.wms.invoice;

import static com.dpvn.shared.util.DateUtil.LOCAL_DATE_FORMAT;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.customer.CustomerService;
import com.dpvn.crm.customer.WebHookHandlerService;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.wmscrudservice.domain.constant.Invoices;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService extends AbstractService {
  private final WmsCrudClient wmsCrudClient;
  private final CustomerService customerService;
  private final CrmCrudClient crmCrudClient;
  private final WebHookHandlerService webHookHandlerService;

  public InvoiceService(
      WmsCrudClient wmsCrudClient,
      CustomerService customerService,
      CrmCrudClient crmCrudClient,
      WebHookHandlerService webHookHandlerService) {
    this.wmsCrudClient = wmsCrudClient;
    this.customerService = customerService;
    this.crmCrudClient = crmCrudClient;
    this.webHookHandlerService = webHookHandlerService;
  }

  public PagingResponse<InvoiceDto> findInvoicesByOptions(
      String filterText,
      Long sellerId,
      Long customerId,
      List<String> statuses,
      int page,
      int pageSize) {
    CustomerDto customerDto = customerService.findCustomerById(customerId);
    return wmsCrudClient.findInvoicesByOptions(
        FastMap.create()
            .add("code", filterText)
            .add("sellerId", sellerId)
            .add("customerId", customerDto.getIdf() == null ? customerId : customerDto.getIdf())
            .add("statuses", statuses)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  public void forceReSyncInvoice(String fromDateStr, String toDateStr) {
    LocalDate fromDate =
        StringUtil.isEmpty(fromDateStr)
            ? LocalDate.parse("01-01-2025", DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT))
            : LocalDate.parse(fromDateStr, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT));
    LocalDate toDate =
        StringUtil.isEmpty(toDateStr)
            ? LocalDate.now()
            : LocalDate.parse(toDateStr, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT));
    LOGGER.info("Forcing re-sync invoice from {} to {}", fromDate, toDate);
    List<InvoiceDto> invoiceDtos =
        wmsCrudClient
            .findInvoicesByOptions(
                FastMap.create()
                    .add("from", fromDate)
                    .add("to", toDate)
                    .add(
                        "statuses",
                        List.of(
                            Invoices.Status.DELIVERING,
                            Invoices.Status.COMPLETED,
                            Invoices.Status.CONFIRMED))
                    .add("page", 0)
                    .add("pageSize", Globals.Paging.MAX_FETCHING_PAGE_SIZE))
            .getRows();

    Map<Long, UserDto> sellers =
        crmCrudClient.getUsers(-1, -1).getRows().stream()
            .collect(Collectors.toMap(UserDto::getIdf, u -> u));
    invoiceDtos.forEach(
        invoiceDto -> {
          UserDto sale = sellers.get(invoiceDto.getSellerId());
          CustomerDto customerDto =
              customerService.findCustomerByKvCustomerId(invoiceDto.getCustomerId());
          if (customerDto != null) {
            if (Invoices.Status.COMPLETED.equals(invoiceDto.getStatus())) {
              webHookHandlerService.handleCompletedInvoice(
                  sale, customerDto, invoiceDto.getCode(), invoiceDto.getPurchaseDate());
            } else {
              webHookHandlerService.handleInProgressInvoice(
                  sale, customerDto, invoiceDto.getCode(), invoiceDto.getPurchaseDate());
            }
          }
        });

    LOGGER.info(
        "Forced re-sync invoice from {} to {} with {} invoices",
        fromDate,
        toDate,
        invoiceDtos.size());
  }
}
