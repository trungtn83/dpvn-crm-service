package com.dpvn.crm.report;

import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
public class SaleReportController {
  private final SaleReportService saleReportService;

  public SaleReportController(SaleReportService saleReportService) {
    this.saleReportService = saleReportService;
  }

  @GetMapping("/sale/{id}")
  public FastMap saleReport(@PathVariable long id, @RequestParam String fromDate, @RequestParam String toDate) {
    return saleReportService.report(id, fromDate, toDate);
  }
}
