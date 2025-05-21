package com.dpvn.crm.webhook;

import com.dpvn.shared.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/webhook")
public class WebHookController {
  private static final Logger LOG = LoggerFactory.getLogger(WebHookController.class);
  private final WebHookService webHookService;

  public WebHookController(WebHookService webHookService) {
    this.webHookService = webHookService;
  }

  @PostMapping("/invoice/{invoiceCode}/re-process")
  public FastMap handleKiotVietWebhookInvoiceReProcess(
      @RequestHeader(defaultValue = "106558L") Long branchId, @PathVariable String invoiceCode) {
    LOG.info("Payload invoice re-process: {}", invoiceCode);
    webHookService.syncInvoicesIfNeed(branchId, List.of(invoiceCode));
    return FastMap.create();
  }

  @PostMapping("/manual-sync")
  public void manualSync(@RequestParam(defaultValue = "7") Integer limit) {
    LOG.info("Manual sync");
    webHookService.manualSync(limit);
  }
}
