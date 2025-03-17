package com.dpvn.crm.voip24h;

import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/voip24h")
public class ViCallLogController {

  private final ViCallLogService viCallLogService;

  public ViCallLogController(ViCallLogService viCallLogService) {
    this.viCallLogService = viCallLogService;
  }

  @PostMapping("/calllog/sync-all")
  public void syncAllViCallLogs() {
    viCallLogService.syncAllCallLogs();
  }

  @GetMapping("/configuration/sync")
  public List<FastMap> getAllConfigurationForSyncList() {
    return viCallLogService.getAllConfigurationForSyncList();
  }
}
