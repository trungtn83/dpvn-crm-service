package com.dpvn.crm.webhook;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("production")
@Component
public class OrderScheduler {
  private final WebHookService webHookService;

  public OrderScheduler(WebHookService webHookService) {
    this.webHookService = webHookService;
  }

  /*
    In the kiotviet, tranngocm start at : 19/04/2025 15:50 (based on ORDER)
  */
  @Scheduled(cron = "0 30 7,9,10,11,12,13,14,15,16,17,18 * * *", zone = "Asia/Ho_Chi_Minh")
  public void manualSync() {
    webHookService.manualSync(7);
  }
}
