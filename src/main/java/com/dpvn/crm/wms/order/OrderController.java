package com.dpvn.crm.wms.order;

import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.OrderDto;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/order")
public class OrderController {
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * @param body sellerId: Long customerId: Long statuses: List<String> page: int pageSize: int
   * @return
   */
  @PostMapping("/find-by-options")
  public PagingResponse<OrderDto> findOrdersByOptions(@RequestBody FastMap body) {
    String filterText = body.getString("filterText");
    Long sellerId = body.getLong("sellerId");
    Long customerId = body.getLong("customerId");
    List<String> statuses = body.getListClass("statuses", String.class);
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(Globals.Paging.PAGE_SIZE, "pageSize");

    return orderService.findOrdersByOptions(
        filterText, sellerId, customerId, statuses, page, pageSize);
  }

  @PostMapping("/show-invoices")
  public List<String> showOrderInvoicesFromMisa(@RequestBody List<FastMap> refs) {
    return orderService.showOrderInvoicesFromMisa(refs);
  }
}
