package com.dpvn.crm.voip24h;

import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voip24h")
public class ViCallLogController {

  private final ViCallLogService viCallLogService;

  public ViCallLogController(ViCallLogService viCallLogService) {
    this.viCallLogService = viCallLogService;
  }

  @PostMapping("/calllog/sync-all")
  public void syncAllViCallLogs(
      @RequestParam(required = false) String fromDateTime,
      @RequestParam(required = false) String toDateTime) {
    viCallLogService.syncAllCallLogs(fromDateTime, toDateTime);
  }

  @GetMapping("/configuration/sync")
  public List<FastMap> getAllConfigurationForSyncList() {
    return viCallLogService.getAllConfigurationForSyncList();
  }
}
