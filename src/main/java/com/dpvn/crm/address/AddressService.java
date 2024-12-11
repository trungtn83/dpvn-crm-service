package com.dpvn.crm.address;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.AddressDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AddressService {
  private final CrmCrudClient crmCrudClient;

  public AddressService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public List<AddressDto> findAllAddresses() {
    return crmCrudClient.findAllAddresses();
  }
}
