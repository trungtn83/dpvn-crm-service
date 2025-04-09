package com.dpvn.crm.voip24h;

import com.dpvn.crm.helper.ConfigurationService;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voip24h")
public class ViCallLogController {

  private final ViCallLogService viCallLogService;
  private final ConfigurationService configurationService;

  public ViCallLogController(
      ViCallLogService viCallLogService, ConfigurationService configurationService) {
    this.viCallLogService = viCallLogService;
    this.configurationService = configurationService;
  }

  @PostMapping("/calllog/sync-all")
  public void syncAllViCallLogs(
      @RequestParam(required = false) String fromDateTime,
      @RequestParam(required = false) String toDateTime) {
    viCallLogService.syncAllCallLogs(fromDateTime, toDateTime);
  }

  @GetMapping("/configuration/sync")
  public List<FastMap> getAllConfigurationForSyncList() {
    return configurationService.getSyncs();
  }
}
