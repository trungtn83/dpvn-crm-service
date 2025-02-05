package com.dpvn.crm.wms.invoice;

import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import com.dpvn.wmscrudservice.domain.dto.InvoiceDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/invoice")
public class InvoiceController {
  private final InvoiceService invoiceService;

  public InvoiceController(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @PostMapping("/find-by-options")
  public PagingResponse<InvoiceDto> findInvoicesByOptions(@RequestBody FastMap body) {
    Long customerId = body.getLong("customerId");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(Globals.Paging.PAGE_SIZE, "pageSize");

    return invoiceService.getInvoices(customerId, page, pageSize);
  }
}
