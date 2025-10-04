package com.dpvn.crm.webhook;

import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebHookController {
  private static final Logger LOG = LoggerFactory.getLogger(WebHookController.class);
  private final WebHookService webHookService;

  public WebHookController(WebHookService webHookService) {
    this.webHookService = webHookService;
  }

  @PostMapping("/order/{orderCode}/re-process")
  public FastMap handleKiotVietWebhookOrderReProcess(@PathVariable String orderCode) {
    LOG.info("Payload order re-process: {}", orderCode);
    webHookService.syncOrdersIfNeed(106558L, List.of(orderCode));
    return FastMap.create();
  }

  @PostMapping("/invoice/{invoiceCode}/re-process")
  public FastMap handleKiotVietWebhookInvoiceReProcess(@PathVariable String invoiceCode) {
    LOG.info("Payload invoice re-process: {}", invoiceCode);
    webHookService.syncInvoicesIfNeed(List.of(invoiceCode));
    return FastMap.create();
  }

  @PostMapping("/manual-sync")
  public void manualSync(@RequestParam(defaultValue = "7") Integer limit) {
    LOG.info("Manual sync");
    webHookService.manualSync(limit);
  }
}
