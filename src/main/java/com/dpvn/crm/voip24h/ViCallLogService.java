package com.dpvn.crm.voip24h;

import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.voip24h.client.Voip24hClient;
import com.dpvn.crm.voip24h.domain.ViCallLogDto;
import com.dpvn.crm.voip24h.domain.ViResponse;
import com.dpvn.reportcrudservice.domain.dto.ConfigDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.util.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ViCallLogService {
  private final Voip24hClient voip24hClient;
  private final ReportCrudClient reportCrudClient;

  public ViCallLogService(Voip24hClient voip24hClient, ReportCrudClient reportCrudClient) {
    this.voip24hClient = voip24hClient;
    this.reportCrudClient = reportCrudClient;
  }

  public List<FastMap> getAllConfigurationForSyncList() {
    List<FastMap> syncs = ResourceFileUtil.readJsonFile("system-configs.json").getList("sync");

    List<ConfigDto> configDtos =
        reportCrudClient.findConfigBy(
            FastMap.create().add("source", "VOIP24H").add("category", "SYNC"));
    syncs.forEach(
        sync ->
            configDtos.stream()
                .filter(
                    config ->
                        "LAST_SYNC".equals(config.getType())
                            && sync.getString("name").equals(config.getName()))
                .findFirst()
                .ifPresent(
                    configDto -> sync.add("lastUpdated", DateUtil.from(configDto.getValue()))));
    return syncs;
  }

  public void syncAllCallLogs() {
    ViCallLogDto lastSyncCallLog = reportCrudClient.findLastCallTime();
    String from = lastSyncCallLog == null ? null : lastSyncCallLog.getCallDate();
    String to =
        LocalDate.now(DateUtil.LOCAL_ZONE_ID)
            .atStartOfDay()
            .plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    int page = 0;
    while (syncCallLog(page, extractDate(from), extractDate(to))) {
      page++;
    }

    ConfigDto configDto = new ConfigDto();
    configDto.setSource("VOIP24H");
    configDto.setCategory("SYNC");
    configDto.setType("LAST_SYNC");
    configDto.setName("CALLLOG");
    configDto.setValue(DateUtil.now().toString());
    reportCrudClient.createConfig(configDto);
  }

  private String extractDate(String dateTime) {
    if (StringUtil.isEmpty(dateTime)) {
      return null;
    }
    List<String> parts = StringUtil.split(dateTime, " ");
    return parts.get(0);
  }

  private boolean syncCallLog(int page, String from, String to) {
    FastMap params =
        FastMap.create()
            .add("offset", page * Globals.Paging.FETCHING_PAGE_SIZE)
            .add("limit", Globals.Paging.FETCHING_PAGE_SIZE)
            .add("dateEnd", to);
    if (from != null) {
      params.add("dateStart", from);
    }

    ViResponse response = voip24hClient.getCallHistory(params);
    List<FastMap> datas = response.getData();
    List<ViCallLogDto> callLogs = datas.stream().map(this::transformToCallLogDto).toList();
    reportCrudClient.syncAllCallLogs(callLogs);
    return datas.size() == Globals.Paging.FETCHING_PAGE_SIZE;
  }

  private ViCallLogDto transformToCallLogDto(FastMap data) {
    ViCallLogDto dto = new ViCallLogDto();
    dto.setUuid(data.getString("id"));
    String callId = data.getString("callId");
    dto.setCallId(callId);
    dto.setCallDate(data.getString("callDate"));
    dto.setCaller(data.getString("caller"));
    dto.setCallee(data.getString("callee"));
    dto.setDid(data.getString("did"));
    dto.setExtension(data.getString("extension"));
    dto.setType(data.getString("type"));
    dto.setStatus(data.getString("status"));
    dto.setCallId(data.getString("callId"));
    dto.setDuration(data.getLong("duration"));
    Long billSec = data.getLong("billsec");
    dto.setBillSec(billSec);
    dto.setNote(data.getString("note"));
    if (billSec > 0) {
      String callRecordingUrl = getCallRecordingFile(callId);
      if (StringUtil.isNotEmpty(callRecordingUrl)) {
        dto.setRecording(callRecordingUrl);
        dto.setPlay(callRecordingUrl);
        dto.setePlay(callRecordingUrl);
        dto.setDownload(callRecordingUrl);
      }
    }
    return dto;
  }

  private String getCallRecordingFile(String callId) {
    ViResponse response = voip24hClient.getCallRecording(callId);
    List<FastMap> recordings = response.getData();
    if (ListUtil.isEmpty(recordings)) {
      return null;
    }
    FastMap data = recordings.get(0);
    FastMap media = data.getMap("media");
    if (media == null || media.isEmpty()) {
      return null;
    }
    return media.getString("ogg");
  }
}
