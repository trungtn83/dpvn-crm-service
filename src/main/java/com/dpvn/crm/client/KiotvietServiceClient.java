package com.dpvn.crm.client;

import com.dpvn.kiotviet.domain.KvAddressBookDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvInvoiceDto;
import com.dpvn.kiotviet.domain.KvOrderDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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

  @GetMapping("/web/order/find-all-manual-sync")
  List<KvOrderDto> findAllManualSync(@RequestParam Integer limit);

  @PostMapping("/wcms/customer/sync/{kvCustomerId}")
  void syncCustomer(@PathVariable Long kvCustomerId);

  @PostMapping("/wcms/order/sync/{code}")
  KvOrderDto syncOrder(@PathVariable String code);

  @PostMapping("/wcms/invoice/sync/{code}")
  KvInvoiceDto syncInvoice(@PathVariable String code);
}
