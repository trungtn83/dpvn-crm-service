package com.dpvn.crm.address;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.shared.domain.dto.AddressDto;
import java.util.List;

import com.dpvn.shared.domain.dto.PagingResponse;
import org.springframework.stereotype.Service;

@Service
public class AddressService {
  private final CrmCrudClient crmCrudClient;

  public AddressService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public PagingResponse<AddressDto> findAllAddresses() {
    return crmCrudClient.findAllAddresses(-1, -1);
  }
}
