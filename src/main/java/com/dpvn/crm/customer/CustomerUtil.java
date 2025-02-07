package com.dpvn.crm.customer;

import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;

public class CustomerUtil {
  private CustomerUtil() {}

  public static FastMap getCustomerOwner(
      CustomerDto customerDto, List<SaleCustomerDto> saleCustomerDtos) {
    if (customerDto.getDeleted()) {
      return FastMap.create().add("ownerId", null).add("ownerName", null); // for clearance
    }
    SaleCustomerDto treasure =
        saleCustomerDtos.stream()
            .filter(
                s ->
                    s.getRelationshipType() == 1 && SaleCustomers.Reason.INVOICE == s.getReasonId())
            .findFirst()
            .orElse(null);
    if (treasure != null) {
      return FastMap.create().add("ownerId", treasure.getSaleId()).add("ownerName", "TREASURE");
    }
    SaleCustomerDto gold =
        saleCustomerDtos.stream()
            .filter(s -> s.getRelationshipType() == 1 && List.of(2, 3, 4).contains(s.getReasonId()))
            .findFirst()
            .orElse(null);
    if (gold != null) {
      return FastMap.create().add("ownerId", gold.getSaleId()).add("ownerName", "GOLD");
    }
    SaleCustomerDto selfDig =
        saleCustomerDtos.stream()
            .filter(
                s -> s.getRelationshipType() == 1 && List.of(70, 71, 72).contains(s.getReasonId()))
            .findFirst()
            .orElse(null);
    if (selfDig != null) {
      if (selfDig.getReasonId() == 70 || selfDig.getReasonId() == 71) {
        return FastMap.create().add("ownerId", selfDig.getSaleId()).add("ownerName", "GOLDMINE");
      }
      return FastMap.create().add("ownerId", selfDig.getSaleId()).add("ownerName", "SANDBANK");
    }
    if (!customerDto.getActive()) {
      return FastMap.create().add("ownerId", null).add("ownerName", "SANDBANK");
    }
    return FastMap.create().add("ownerId", null).add("ownerName", "GOLDMINE");
  }

  public static FastMap getCustomerOwner(
      Long saleId, CustomerDto customerDto, List<SaleCustomerDto> saleCustomerDtos) {
    FastMap owner = getCustomerOwner(customerDto, saleCustomerDtos);
    Long ownerId = owner.getLong("ownerId");
    boolean isViewable = saleId == null || ownerId == null || ownerId.equals(saleId);
    return owner.add("isViewable", isViewable);
  }
}
