package com.dpvn.crm.wms.invoice;

import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.customer.CustomerService;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {
  private final WmsCrudClient wmsCrudClient;
  private final CustomerService customerService;

  public InvoiceService(WmsCrudClient wmsCrudClient, CustomerService customerService) {
    this.wmsCrudClient = wmsCrudClient;
    this.customerService = customerService;
  }

  public PagingResponse<InvoiceDto> getInvoices(Long customerId, int page, int pageSize) {
    CustomerDto customerDto = customerService.findCustomerById(customerId);
    return wmsCrudClient.findInvoicesByOptions(
        FastMap.create()
            .add("customerId", customerDto.getIdf() == null ? customerId : customerDto.getIdf())
            .add("page", page)
            .add("pageSize", pageSize));
  }
}
