package com.dpvn.crm.client;

import com.dpvn.kiotviet.domain.KvAddressBookDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvOrderDto;
import com.dpvn.shared.util.FastMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "kiotviet-service", contextId = "kiotviet-service-client")
public interface KiotvietServiceClient {
  @GetMapping("/web/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);

  @PostMapping("/web/customer/create-from-website")
  KvCustomerDto createCustomerFromWebsite(@RequestBody KvCustomerDto kvCustomerDto);

  @PostMapping("/web/customer/address/create-from-website")
  KvAddressBookDto createCustomerAddressFromWebsite(@RequestBody KvAddressBookDto kvAddressBookDto);

  @PostMapping("/web/order/create-from-website")
  KvOrderDto createOrderFromWebsite(@RequestBody FastMap order);

  @PostMapping("/wcms/customer/sync/{kvCustomerId}")
  void syncCustomer(@PathVariable Long kvCustomerId);

  @PostMapping("/wcms/order/sync/{code}")
  void syncOrder(@PathVariable String code);

  @PostMapping("/wcms/invoice/sync/{code}")
  void syncInvoice(@PathVariable String code);
}
