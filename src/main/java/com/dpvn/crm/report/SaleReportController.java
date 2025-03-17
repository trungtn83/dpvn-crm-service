package com.dpvn.crm.report;

import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class SaleReportController {
  private final SaleReportService saleReportService;

  public SaleReportController(SaleReportService saleReportService) {
    this.saleReportService = saleReportService;
  }

  @GetMapping("/sale/{id}")
  public FastMap saleDetailReport(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestParam String fromDate,
      @RequestParam String toDate) {
    return saleReportService.reportSaleDetail(loginUserId, id, fromDate, toDate);
  }

  @GetMapping("/sale")
  public List<FastMap> salesReport(
      @RequestHeader("x-user-id") Long loginUserId,
      @RequestParam String fromDate,
      @RequestParam String toDate) {
    return saleReportService.reportSales(loginUserId, fromDate, toDate);
  }
}
