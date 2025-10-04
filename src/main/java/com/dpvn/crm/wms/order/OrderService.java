package com.dpvn.crm.wms.order;

import com.dpvn.crm.client.MisaAmisServiceClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.customer.CustomerService;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.OrderDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService extends AbstractService {
  private final WmsCrudClient wmsCrudClient;
  private final CustomerService customerService;
  private final MisaAmisServiceClient misaAmisServiceClient;

  public OrderService(WmsCrudClient wmsCrudClient, CustomerService customerService, MisaAmisServiceClient misaAmisServiceClient) {
    this.wmsCrudClient = wmsCrudClient;
    this.customerService = customerService;
    this.misaAmisServiceClient = misaAmisServiceClient;
  }

  public PagingResponse<OrderDto> findOrdersByOptions(
      String filterText,
      Long sellerId,
      Long customerId,
      List<String> statuses,
      int page,
      int pageSize) {
    CustomerDto customerDto = customerService.findCustomerById(customerId);
    return wmsCrudClient.findOrdersByOptions(
        FastMap.create()
            .add("code", filterText)
            .add("sellerId", sellerId)
            .add("customerId", customerDto.getIdf() == null ? customerId : customerDto.getIdf())
            .add("statuses", statuses)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  public List<String> showOrderInvoicesFromMisa(List<FastMap> refs) {
    return misaAmisServiceClient.getInvoiceShow(refs);
  }
}
