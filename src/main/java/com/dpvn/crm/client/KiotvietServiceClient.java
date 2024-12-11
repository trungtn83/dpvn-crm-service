package com.dpvn.crm.client;

import com.dpvn.reportcrudservice.domain.dto.kiotviet.KvCustomerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kiotviet-service", contextId = "kiotviet-service-client")
public interface KiotvietServiceClient {
  @GetMapping("/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);
}
