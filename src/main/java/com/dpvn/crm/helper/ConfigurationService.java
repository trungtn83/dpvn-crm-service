package com.dpvn.crm.helper;

import com.dpvn.crm.campaign.Dispatchs;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.reportcrudservice.domain.dto.ConfigDto;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ResourceFileUtil;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {
  private final ReportCrudClient reportCrudClient;

  public ConfigurationService(ReportCrudClient reportCrudClient) {
    this.reportCrudClient = reportCrudClient;
  }

  public List<FastMap> getSyncs() {
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

  public void addConfigLastSync(String source, String name) {
    ConfigDto configDto = new ConfigDto();
    configDto.setSource(source);
    configDto.setCategory("SYNC");
    configDto.setType("LAST_SYNC");
    configDto.setName(name);
    configDto.setValue(DateUtil.now().toString());
    configDto.setActive(Boolean.TRUE);
    configDto.setDeleted(Boolean.FALSE);
    reportCrudClient.upsertConfig(configDto);
  }

  /**
   * source: CRM
   * category: DISPATCH
   * name: ROUND_ROBIN
   * type: LAST_SALE_ID
   * value: saleId
   */
  public void upsertConfig(String source, String category, String name, String type, String value) {
    ConfigDto configDto = new ConfigDto();
    configDto.setSource(source);
    configDto.setCategory(category);
    configDto.setType(type);
    configDto.setName(name);
    configDto.setValue(value);
    configDto.setActive(Boolean.TRUE);
    configDto.setDeleted(Boolean.FALSE);
    reportCrudClient.upsertConfig(configDto);
  }

  public String getConfigDispatchMethod() {
    List<ConfigDto> configDtos =
        reportCrudClient.findConfigBy(
            FastMap.create()
                .add("source", "CRM")
                .add("category", "DISPATCH")
                .add("name", "METHOD")
                .add("type", "DEFAULT"));
    if (ListUtil.isEmpty(configDtos)) {
      return Dispatchs.Method.ROUND_ROBIN;
    }
    return configDtos.get(0).getValue();
  }

  public Long getConfigDispatchRoundRobin() {
    List<ConfigDto> configDtos =
        reportCrudClient.findConfigBy(
            FastMap.create()
                .add("source", "CRM")
                .add("category", "DISPATCH")
                .add("name", "ROUND_ROBIN")
                .add("type", "LAST_SALE_ID"));
    if (ListUtil.isEmpty(configDtos)) {
      return 0L;
    }
    return Long.parseLong(configDtos.get(0).getValue());
  }

  public void upsertConfigDispatchRoundRobin(Long userId) {
    upsertConfig("CRM", "DISPATCH", "ROUND_ROBIN", "LAST_SALE_ID", userId.toString());
  }
}
