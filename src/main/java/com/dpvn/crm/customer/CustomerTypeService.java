package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.CustomerTypeDto;
import com.dpvn.shared.service.AbstractService;
import java.util.List;
import org.springframework.stereotype.Service;

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
