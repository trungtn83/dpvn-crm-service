package com.dpvn.crm.address;

import com.dpvn.shared.domain.dto.AddressDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/address")
public class AddressController {
  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @GetMapping
  public PagingResponse<AddressDto> findAllAddresses() {
    return addressService.findAllAddresses();
  }
}
