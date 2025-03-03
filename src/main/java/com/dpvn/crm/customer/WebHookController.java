package com.dpvn.crm.customer;

import com.dpvn.shared.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebHookController {
  private static final Logger LOG = LoggerFactory.getLogger(WebHookController.class);
  private final WebHookService webHookService;

  public WebHookController(WebHookService webHookService) {
    this.webHookService = webHookService;
  }

  @PostMapping("/order")
  public void handleKiotVietWebhookOrderUpdate(@RequestBody String payload) {
    LOG.info("Payload order: {}", payload);
    webHookService.processOrder(payload);
  }

  @PostMapping("/invoice")
  public void handleKiotVietWebhookInvoiceUpdate(@RequestBody String payload) {
    LOG.info("Payload invoice: {}", payload);
    webHookService.processInvoice(payload);
  }

  @PostMapping("/invoice/{invoiceCode}/re-process")
  public FastMap handleKiotVietWebhookInvoiceReProcess(@PathVariable String invoiceCode) {
    LOG.info("Payload invoice re-process: {}", invoiceCode);
    return webHookService.reprocessInvoice(invoiceCode);
  }
}
