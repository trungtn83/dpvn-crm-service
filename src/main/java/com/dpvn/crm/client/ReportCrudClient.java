package com.dpvn.crm.client;

import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.reportcrudservice.domain.dto.LogDto;
import com.dpvn.reportcrudservice.domain.dto.kiotviet.KvCustomerDto;
import com.dpvn.reportcrudservice.domain.dto.kiotviet.KvUserDto;
import com.dpvn.shared.util.FastMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "report-crud-service", contextId = "report-crud-service-client")
public interface ReportCrudClient {
  @PostMapping("/log")
  void createLog(@RequestBody LogDto logDtp);

  @GetMapping("/kiotviet/user/{id}")
  KvUserDto findKvUserById(@PathVariable("id") Long id);

  @GetMapping("/kiotviet/customer/{id}")
  KvCustomerDto findKvCustomerById(@PathVariable("id") Long id);
}
