package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.constant.RelationshipType;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
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

  public void createNewSaleCustomer(SaleCustomerDto saleCustomerDto) {
    crmCrudClient.createNewSaleCustomer(saleCustomerDto);
  }

  public void updateExistedSaleCustomer(Long id, FastMap saleCustomerDto) {
    crmCrudClient.updateExistedSaleCustomer(id, saleCustomerDto);
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
      Long saleId, Long customerId, Integer relationshipType, Integer reasonId, String reasonRef) {
    FastMap options =
        FastMap.create()
            .add("saleId", saleId)
            .add("customerIds", List.of(customerId))
            .add("relationshipType", relationshipType)
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

  public void doActionCustomer(Long saleId, Long customerId, Integer reasonId, boolean flag) {
    SaleCustomerDto saleCustomerDto =
        findSaleCustomerByReason(saleId, customerId, RelationshipType.INVOLVED, reasonId, null);
    if (flag) {
      if (saleCustomerDto == null) {
        SaleCustomerDto newSaleCustomerDto = new SaleCustomerDto();
        newSaleCustomerDto.setSaleId(saleId);
        newSaleCustomerDto.setCustomerId(customerId);
        newSaleCustomerDto.setRelationshipType(RelationshipType.INVOLVED);
        newSaleCustomerDto.setReasonId(reasonId);
        newSaleCustomerDto.setActive(Boolean.TRUE);
        createNewSaleCustomer(newSaleCustomerDto);
      }
    } else {
      if (saleCustomerDto != null) {
        removeSaleCustomerByReason(saleId, customerId, reasonId, null);
      }
    }
  }
}
