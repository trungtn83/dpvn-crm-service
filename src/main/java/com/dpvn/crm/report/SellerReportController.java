package com.dpvn.crm.report;

import com.dpvn.crm.report.domain.PerformanceBySellerDetail;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.LocalDateUtil;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class SellerReportController {
  private final SellerReportService sellerReportService;

  public SellerReportController(SellerReportService sellerReportService) {
    this.sellerReportService = sellerReportService;
  }

  @GetMapping("/sale/{id}")
  public List<PerformanceBySellerDetail> saleDetailReport(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestParam String fromDate,
      @RequestParam String toDate) {
    return sellerReportService.reportSaleDetail(
        loginUserId, id, fromDate, LocalDateUtil.toString(LocalDateUtil.from(toDate).plusDays(1)));
  }

  @GetMapping("/sale")
  public List<FastMap> salesReport(
      @RequestHeader("x-user-id") Long loginUserId,
      @RequestParam String fromDate,
      @RequestParam String toDate) {
    return sellerReportService.reportSales(
        loginUserId, fromDate, LocalDateUtil.toString(LocalDateUtil.from(toDate).plusDays(1)));
  }
}
