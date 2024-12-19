package com.dpvn.crm.client;

import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "report-crud-service", contextId = "report-crud-service-client")
public interface ReportCrudClient {
  @GetMapping("/kiotviet/user/{id}")
  KvUserDto findKvUserById(@PathVariable("id") Long id);

  @GetMapping("/kiotviet/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);
}
