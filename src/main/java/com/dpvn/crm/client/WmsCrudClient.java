package com.dpvn.crm.client;

import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import com.dpvn.wmscrudservice.domain.dto.OrderDto;
import com.dpvn.wmscrudservice.domain.entity.report.InvoiceBySeller;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "wms-crud-service", contextId = "wms-crud-service-client")
public interface WmsCrudClient {
  @PostMapping("/order/find-not-existed-from-list")
  List<String> findNotExistedOrderFromList(
      @RequestHeader Long branchId, @RequestBody List<String> orderCodes);

  /**
   * @param body - status: String
   *             - customerIds: List<Long>
   * @return - customerId: Long
   * - orderCode: String
   * - purchaseDate: Instant
   * - sellerId: Long
   */
  @PostMapping("/order/find-last-purchase-by-status-and-customers")
  List<FastMap> findLastPurchaseOrderByStatusAndCustomers(@RequestBody FastMap body);

  @PostMapping("/order/sync-all")
  void syncAllOrders(@RequestBody List<OrderDto> orderDtos);

  /**
   * code: String
   * sellerId: Long -> idf
   * customerId: Long -> idf
   * statuses: List<String>
   * from: String -> yyyy-MM-dd
   * to: String -> yyyy-MM-dd
   * page: int
   * pageSize: int
   */
  @PostMapping("/invoice/find-by-options")
  PagingResponse<InvoiceDto> findInvoicesByOptions(@RequestBody FastMap body);

  /**
   * sellerIds: List<Long>
   * fromDate: yyyy-MM-dd
   * toDate: yyyy-MM-dd
   */
  @PostMapping("/invoice/report-by-sellers")
  Map<Long, List<InvoiceBySeller>> reportInvoicesBySellers(@RequestBody FastMap body);

  /**
   * @param body - status: String
   *             - customerIds: List<Long>
   * @return - customerId: Long
   * - orderCode: String
   * - purchaseDate: Instant
   * - sellerId: Long
   */
  @PostMapping("/invoice/find-last-purchase-by-status-and-customers")
  List<FastMap> findLastPurchaseInvoiceByStatusAndCustomers(@RequestBody FastMap body);

  @PostMapping("/invoice/sync-all")
  void syncAllInvoices(@RequestBody List<InvoiceDto> invoiceDtos);

  @PostMapping("/invoice/find-not-existed-from-list")
  List<String> findNotExistedInvoiceFromList(
      @RequestHeader Long branchId, @RequestBody List<String> orderCodes);

  /**
   * @param body - status
   *             - codes
   */
  @PostMapping("/invoice/update-batch-status")
  void updateBatchInvoiceStatus(@RequestBody FastMap body);
}
