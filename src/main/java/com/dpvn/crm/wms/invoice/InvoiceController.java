package com.dpvn.crm.wms.invoice;

import com.dpvn.crm.user.UserService;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/invoice")
public class InvoiceController {
  private final InvoiceService invoiceService;
  private final UserService userService;

  public InvoiceController(InvoiceService invoiceService, UserService userService) {
    this.invoiceService = invoiceService;
    this.userService = userService;
  }

  /**
   * sellerId: Long
   * customerId: Long
   * statuses: List<String>
   * page: int
   * pageSize: int
   */
  @PostMapping("/find-by-options")
  public PagingResponse<InvoiceDto> findInvoicesByOptions(@RequestBody FastMap body) {
    String filterText = body.getString("filterText");
    Long sellerId = body.getLong("sellerId");
    Long customerId = body.getLong("customerId");
    List<String> statuses = body.getListClass("statuses", String.class);
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(Globals.Paging.PAGE_SIZE, "pageSize");

    return invoiceService.findInvoicesByOptions(
        filterText, sellerId, customerId, statuses, page, pageSize);
  }
}
