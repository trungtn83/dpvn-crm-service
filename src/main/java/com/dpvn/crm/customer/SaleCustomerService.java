package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SaleCustomerService {
  private final CrmCrudClient crmCrudClient;

  public SaleCustomerService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public void upsertSaleCustomer(SaleCustomerDto saleCustomerDto) {
    crmCrudClient.upsertSaleCustomer(saleCustomerDto);
  }

  public void removeSaleCustomerByReason(
      Long saleId, Long customerId, Integer reasonId, String reasonRef) {
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setSaleId(saleId);
    saleCustomerDto.setCustomerId(customerId);
    saleCustomerDto.setReasonId(reasonId);
    saleCustomerDto.setReasonRef(reasonRef);
    crmCrudClient.removeSaleCustomerByOptions(saleCustomerDto);
  }

  public SaleCustomerDto findSaleCustomerByReason(
      Long saleId, Long customerId, Integer type, Integer reasonId, String reasonRef) {
    FastMap options =
        FastMap.create()
            .add("saleId", saleId)
            .add("customerIds", List.of(customerId))
            .add("relationshipType", type)
            .add("reasonRef", reasonRef);
    if (reasonId != null) {
      options.add("reasonIds", List.of(reasonId));
    }
    List<SaleCustomerDto> saleCustomerDtos = crmCrudClient.findSaleCustomersByOptions(options);
    if (ListUtil.isEmpty(saleCustomerDtos)) {
      return null;
    }
    return saleCustomerDtos.get(0);
  }

  public SaleCustomerStateDto getSaleCustomerStateBySale(Long userId, Long customerId) {
    return crmCrudClient.getSaleCustomerStateBySale(userId, customerId);
  }
}
