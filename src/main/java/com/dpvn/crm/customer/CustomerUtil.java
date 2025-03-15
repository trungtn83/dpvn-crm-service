package com.dpvn.crm.customer;

import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
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
      return FastMap.create()
          .add("ownerId", List.of(treasure.getSaleId()))
          .add("ownerName", Customers.Owner.TREASURE);
    }
    SaleCustomerDto gold =
        saleCustomerDtos.stream()
            .filter(s -> s.getRelationshipType() == 1 && List.of(2, 3, 4).contains(s.getReasonId()))
            .findFirst()
            .orElse(null);
    if (gold != null) {
      return FastMap.create()
          .add("ownerId", List.of(gold.getSaleId()))
          .add("ownerName", Customers.Owner.GOLD);
    }
    List<SaleCustomerDto> selfDigs =
        saleCustomerDtos.stream()
            .filter(
                s -> s.getRelationshipType() == 1 && List.of(70, 71, 72).contains(s.getReasonId()))
            .toList();
    if (ListUtil.isNotEmpty(selfDigs)) {
      List<Long> saleIds = selfDigs.stream().map(SaleCustomerDto::getSaleId).toList();
      if (selfDigs.get(0).getReasonId() == 70 || selfDigs.get(0).getReasonId() == 71) {
        return FastMap.create().add("ownerId", saleIds).add("ownerName", Customers.Owner.GOLDMINE);
      }
      return FastMap.create().add("ownerId", saleIds).add("ownerName", Customers.Owner.SANDBANK);
    }
    if (!customerDto.getActive()) {
      return FastMap.create().add("ownerId", null).add("ownerName", Customers.Owner.SANDBANK);
    }
    return FastMap.create().add("ownerId", null).add("ownerName", Customers.Owner.GOLDMINE);
  }

  public static FastMap getCustomerOwner(
      Long saleId, CustomerDto customerDto, List<SaleCustomerDto> saleCustomerDtos) {
    FastMap owner = getCustomerOwner(customerDto, saleCustomerDtos);
    List<Long> ownerId = owner.getListClass("ownerId", Long.class);
    String ownerName = owner.getString("ownerName");
    boolean isViewable =
        saleId == null // is god by role or is account by department
            || (List.of(Customers.Owner.SANDBANK, Customers.Owner.GOLDMINE)
                .contains(ownerName)) // is in bãi cát hoặc mỏ vàng
            || ListUtil.isEmpty(ownerId) // not belong to anyone
            || ownerId.contains(saleId); // belong to me
    return owner.add("isViewable", isViewable);
  }
}
