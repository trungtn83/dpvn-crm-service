package com.dpvn.crm.client;

import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvUserDto;
import com.dpvn.shared.util.FastMap;
import com.dpvn.thuocsi.domain.TsAddressDto;
import com.dpvn.thuocsi.domain.TsCustomerDto;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
}
