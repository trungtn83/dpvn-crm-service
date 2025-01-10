package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.CustomerTypeDto;
import com.dpvn.shared.service.AbstractService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerTypeService extends AbstractService {
  private final CrmCrudClient crmCrudClient;

  public CustomerTypeService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public List<CustomerTypeDto> getAllCustomerTypes() {
    return crmCrudClient.getAllCustomerTypes();
  }
}
