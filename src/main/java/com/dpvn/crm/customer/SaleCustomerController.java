package com.dpvn.crm.customer;

import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sale-customer")
public class SaleCustomerController {
  private final SaleCustomerService saleCustomerService;

  public SaleCustomerController(SaleCustomerService saleCustomerService) {
    this.saleCustomerService = saleCustomerService;
  }

  @PostMapping
  public void createSaleCustomer(@RequestBody SaleCustomerDto dto) {
    saleCustomerService.createNewSaleCustomer(dto);
  }

  @PostMapping("/{id}")
  public void updateExistedSaleCustomer(
      @PathVariable Long id, @RequestBody FastMap saleCustomerDto) {
    saleCustomerService.updateExistedSaleCustomer(id, saleCustomerDto);
  }
}
