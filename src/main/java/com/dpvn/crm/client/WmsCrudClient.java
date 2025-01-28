package com.dpvn.crm.client;

import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import com.dpvn.wmscrudservice.domain.dto.OrderDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wms-crud-service", contextId = "wms-crud-service-client")
public interface WmsCrudClient {
  /**
   * @param body - status: String - customerIds: List<Long>
   * @return - customerId: Long - orderCode: String - purchaseDate: Instant - sellerId: Long
   */
  @PostMapping("/order/find-last-purchase-by-status-and-customers")
  List<FastMap> findLastPurchaseOrderByStatusAndCustomers(@RequestBody FastMap body);

  @PostMapping("/order/sync-all")
  void syncAllOrders(@RequestBody List<OrderDto> orderDtos);

  /**
   *
   * @param body
   *  - sellerId: Long -> idf
   *  - page: int
   *  - pageSize: int
   */
  @PostMapping("/invoice/find-by-options")
  PagingResponse<InvoiceDto> findInvoicesByOptions(@RequestBody FastMap body);

  /**
   * @param body - status: String - customerIds: List<Long>
   * @return - customerId: Long - orderCode: String - purchaseDate: Instant - sellerId: Long
   */
  @PostMapping("/invoice/find-last-purchase-by-status-and-customers")
  List<FastMap> findLastPurchaseInvoiceByStatusAndCustomers(@RequestBody FastMap body);

  @PostMapping("/invoice/sync-all")
  void syncAllInvoices(@RequestBody List<InvoiceDto> invoiceDtos);

}
