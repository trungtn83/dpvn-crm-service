package com.dpvn.crm.client;

import com.dpvn.kiotviet.domain.KvCustomerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "kiotviet-service", contextId = "kiotviet-service-client")
public interface KiotvietServiceClient {
  @GetMapping("/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);

  @PostMapping("/wcms/customer/sync/{id}")
  void syncCustomer(@PathVariable Long id);

  @PostMapping("/wcms/order/sync/{code}")
  void syncOrder(@PathVariable String code);

  @PostMapping("/wcms/invoice/sync/{code}")
  void syncInvoice(@PathVariable String code);
}
