package com.dpvn.crm.client;

import com.dpvn.crm.voip24h.domain.ViCallLogDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvUserDto;
import com.dpvn.reportcrudservice.domain.dto.ConfigDto;
import com.dpvn.shared.util.FastMap;
import com.dpvn.thuocsi.domain.TsAddressDto;
import com.dpvn.thuocsi.domain.TsCustomerDto;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "report-crud-service", contextId = "report-crud-service-client")
public interface ReportCrudClient {
  @GetMapping("/kiotviet/user/{id}")
  KvUserDto findKvUserById(@PathVariable("id") Long id);

  @GetMapping("/kiotviet/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);

  // TODO: body
  // - lastCreatedDate: Instant
  // - page: Integer
  // - pageSize: Integer
  @PostMapping("/kiotviet/customer/find-for-sync")
  List<KvCustomerDto> findForSync(@RequestBody FastMap body);

  @GetMapping("/thuocsi/customer")
  List<TsCustomerDto> findTsCustomers(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize);

  @Cacheable(value = "ts-customers", key = "#root.methodName")
  @GetMapping("/thuocsi/address")
  List<TsAddressDto> findAllTsAddresses();

  @PostMapping("/voip24h/calllog/sync-all")
  void syncAllCallLogs(@RequestBody List<ViCallLogDto> dtos);

  @GetMapping("/voip24h/calllog/latest-call-time")
  ViCallLogDto findLastCallTime();

  @GetMapping("/voip24h/calllog/report/{caller}")
  List<ViCallLogDto> findCallLogsByCaller(
      @PathVariable("caller") String caller,
      @RequestParam String fromDate,
      @RequestParam String toDate);

  @PostMapping("/system/config/find-by")
  List<ConfigDto> findConfigBy(@RequestBody FastMap body);

  @PostMapping("/system/config/upsert-value")
  void createConfig(@RequestBody ConfigDto configDto);
}
