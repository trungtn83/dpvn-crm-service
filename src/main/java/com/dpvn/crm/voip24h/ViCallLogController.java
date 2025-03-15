package com.dpvn.crm.voip24h;

import com.dpvn.shared.config.CacheService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vi-call-log")
public class ViCallLogController {

  private final ViCallLogService viCallLogService;
  private final CacheService cacheService;

  public ViCallLogController(ViCallLogService viCallLogService, CacheService cacheService) {
    this.viCallLogService = viCallLogService;
    this.cacheService = cacheService;
  }

  @PostMapping("/sync-all")
  public void syncAllViCallLogs() {
    viCallLogService.syncAllCallLogs();
  }
}
